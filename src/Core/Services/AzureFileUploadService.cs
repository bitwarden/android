using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Threading.Tasks;
using System.Web;
using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;
using Bit.Core.Utilities;

namespace Bit.Core.Services
{
    public class AzureFileUploadService : IAzureFileUploadService
    {
        private const long MAX_SINGLE_BLOB_UPLOAD_SIZE = 256 * 1024 * 1024; // 256 MiB
        private const int MAX_BLOCKS_PER_BLOB = 50000;
        private const decimal MAX_MOBILE_BLOCK_SIZE = 5 * 1024 * 1024; // 5 MB

        private readonly HttpClient _httpClient = new HttpClient();

        public AzureFileUploadService()
        {
            _httpClient.DefaultRequestHeaders.CacheControl = new CacheControlHeaderValue()
            {
                NoCache = true,
            };
        }

        public async Task Upload(string uri, EncByteArray data, Func<Task<string>> renewalCallback)
        {
            if (data?.Buffer?.Length <= MAX_SINGLE_BLOB_UPLOAD_SIZE)
            {
                await AzureUploadBlob(uri, data);
            }
            else
            {
                await AzureUploadBlocks(uri, data, renewalCallback);
            }
        }

        private async Task AzureUploadBlob(string uri, EncByteArray data)
        {
            using (var requestMessage = new HttpRequestMessage())
            {
                var uriBuilder = new UriBuilder(uri);
                var paramValues = HttpUtility.ParseQueryString(uriBuilder.Query);

                requestMessage.Headers.Add("x-ms-date", DateTime.UtcNow.ToString("R"));
                requestMessage.Headers.Add("x-ms-version", paramValues["sv"]);
                requestMessage.Headers.Add("x-ms-blob-type", "BlockBlob");

                requestMessage.Content = new ByteArrayContent(data.Buffer);
                requestMessage.Version = new Version(1, 0);
                requestMessage.Method = HttpMethod.Put;
                requestMessage.RequestUri = uriBuilder.Uri;

                var blobResponse = await _httpClient.SendAsync(requestMessage);

                if (blobResponse.StatusCode != HttpStatusCode.Created)
                {
                    throw new Exception("Failed to create Azure blob");
                }
            }
        }

        private async Task AzureUploadBlocks(string uri, EncByteArray data, Func<Task<string>> renewalFunc)
        {
            _httpClient.Timeout = TimeSpan.FromHours(3);
            var baseParams = HttpUtility.ParseQueryString(CoreHelpers.GetUri(uri).Query);
            var blockSize = MaxBlockSize(baseParams["sv"]);
            var blockIndex = 0;
            var numBlocks = Math.Ceiling((decimal)data.Buffer.Length / blockSize);
            var blocksStaged = new List<string>();

            if (numBlocks > MAX_BLOCKS_PER_BLOB)
            {
                throw new Exception($"Cannot upload file, exceeds maximum size of {blockSize * MAX_BLOCKS_PER_BLOB}");
            }

                while (blockIndex < numBlocks)
                {
                    uri = await RenewUriIfNecessary(uri, renewalFunc);
                    var blockUriBuilder = new UriBuilder(uri);
                    var blockId = EncodeBlockId(blockIndex);
                    var blockParams = HttpUtility.ParseQueryString(blockUriBuilder.Query);
                    blockParams.Add("comp", "block");
                    blockParams.Add("blockid", blockId);
                    blockUriBuilder.Query = blockParams.ToString();

                    using (var requestMessage = new HttpRequestMessage())
                    {
                        requestMessage.Headers.Add("x-ms-date", DateTime.UtcNow.ToString("R"));
                        requestMessage.Headers.Add("x-ms-version", baseParams["sv"]);
                        requestMessage.Headers.Add("x-ms-blob-type", "BlockBlob");

                        requestMessage.Content = new ByteArrayContent(data.Buffer.Skip(blockIndex * blockSize).Take(blockSize).ToArray());
                        requestMessage.Version = new Version(1, 0);
                        requestMessage.Method = HttpMethod.Put;
                        requestMessage.RequestUri = blockUriBuilder.Uri;

                        var blockResponse = await _httpClient.SendAsync(requestMessage);

                        if (blockResponse.StatusCode != HttpStatusCode.Created)
                        {
                            throw new Exception("Failed to create Azure block");
                        }
                    }

                    blocksStaged.Add(blockId);
                    blockIndex++;
                }

                using (var requestMessage = new HttpRequestMessage())
                {
                    uri = await RenewUriIfNecessary(uri, renewalFunc);
                    var blockListXml = GenerateBlockListXml(blocksStaged);
                    var blockListUriBuilder = new UriBuilder(uri);
                    var blockListParams = HttpUtility.ParseQueryString(blockListUriBuilder.Query);
                    blockListParams.Add("comp", "blocklist");
                    blockListUriBuilder.Query = blockListParams.ToString();

                    requestMessage.Headers.Add("x-ms-date", DateTime.UtcNow.ToString("R"));
                    requestMessage.Headers.Add("x-ms-version", baseParams["sv"]);

                    requestMessage.Content = new StringContent(blockListXml);
                    requestMessage.Version = new Version(1, 0);
                    requestMessage.Method = HttpMethod.Put;
                    requestMessage.RequestUri = blockListUriBuilder.Uri;

                    var blockListResponse = await _httpClient.SendAsync(requestMessage);

                    if (blockListResponse.StatusCode != HttpStatusCode.Created)
                    {
                        throw new Exception("Failed to PUT Azure block list");
                    }
                }
        }

        private async Task<string> RenewUriIfNecessary(string uri, Func<Task<string>> renewalFunc)
        {
            var uriParams = HttpUtility.ParseQueryString(CoreHelpers.GetUri(uri).Query);

            if (DateTime.TryParse(uriParams.Get("se") ?? "", out DateTime expiry) && expiry < DateTime.UtcNow.AddSeconds(1))
            {
                return await renewalFunc();
            }
            return uri;
        }

        private string GenerateBlockListXml(List<string> blocksStaged)
        {
            var xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?><BlockList>");
            foreach(var blockId in blocksStaged)
            {
                xml.Append($"<Latest>{blockId}</Latest>");
            }
            xml.Append("</BlockList>");
            return xml.ToString();
        }

        private string EncodeBlockId(int index)
        {
            // Encoded blockId max size is 64, so pre-encoding max size is 48
            var paddedString = index.ToString("D48");
            return Convert.ToBase64String(Encoding.UTF8.GetBytes(paddedString));
        }

        private int MaxBlockSize(string version)
        {
            long maxSize = 4194304L; // 4 MiB
            if (CompareAzureVersions(version, "2019-12-12") >= 0)
            {
                maxSize = 4194304000L; // 4000 MiB
            }
            else if (CompareAzureVersions(version, "2016-05-31") >= 0)
            {
                maxSize = 104857600L; // 100 MiB
            }

            return maxSize > MAX_MOBILE_BLOCK_SIZE ? (int)MAX_MOBILE_BLOCK_SIZE : (int) maxSize;
        }

        private int CompareAzureVersions(string a, string b)
        {
            var v1Parts = a.Split('-').Select(p => int.Parse(p));
            var v2Parts = b.Split('-').Select(p => int.Parse(p));

            return a[0] != b[0] ? a[0] - b[0] :
                a[1] != b[1] ? a[1] - b[1] :
                a[2] != b[2] ? a[2] - b[2] :
                0;
        }
    }
}
