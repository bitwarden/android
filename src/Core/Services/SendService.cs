using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;
using Bit.Core.Models.Response;
using Bit.Core.Models.View;
using Newtonsoft.Json;

namespace Bit.Core.Services
{
    public class SendService : ISendService
    {
        private List<SendView> _decryptedSendsCache;
        private readonly ICryptoService _cryptoService;
        private readonly IUserService _userService;
        private readonly IApiService _apiService;
        private readonly IStorageService _storageService;
        private readonly II18nService _i18nService;
        private readonly ICryptoFunctionService _cryptoFunctionService;
        private Task<List<SendView>> _getAllDecryptedTask;
        private readonly IFileUploadService _fileUploadService;

        public SendService(
            ICryptoService cryptoService,
            IUserService userService,
            IApiService apiService,
            IFileUploadService fileUploadService,
            IStorageService storageService,
            II18nService i18nService,
            ICryptoFunctionService cryptoFunctionService)
        {
            _cryptoService = cryptoService;
            _userService = userService;
            _apiService = apiService;
            _fileUploadService = fileUploadService;
            _storageService = storageService;
            _i18nService = i18nService;
            _cryptoFunctionService = cryptoFunctionService;
        }

        public static string GetSendKey(string userId) => string.Format("sends_{0}", userId);

        public async Task ClearAsync(string userId)
        {
            await _storageService.RemoveAsync(GetSendKey(userId));
            ClearCache();
        }

        public void ClearCache() => _decryptedSendsCache = null;

        public async Task DeleteAsync(params string[] ids)
        {
            var userId = await _userService.GetUserIdAsync();
            var sends = await _storageService.GetAsync<Dictionary<string, SendData>>(GetSendKey(userId));

            if (sends == null)
            {
                return;
            }

            foreach (var id in ids)
            {
                sends.Remove(id);
            }

            await _storageService.SaveAsync(GetSendKey(userId), sends);
            ClearCache();
        }

        public async Task DeleteWithServerAsync(string id)
        {
            await _apiService.DeleteSendAsync(id);
            await DeleteAsync(id);
        }

        public async Task<(Send send, EncByteArray encryptedFileData)> EncryptAsync(SendView model, byte[] fileData,
            string password, SymmetricCryptoKey key = null)
        {
            if (model.Key == null)
            {
                model.Key = _cryptoFunctionService.RandomBytes(16);
                model.CryptoKey = await _cryptoService.MakeSendKeyAsync(model.Key);
            }

            var send = new Send
            {
                Id = model.Id,
                Type = model.Type,
                Disabled = model.Disabled,
                DeletionDate = model.DeletionDate,
                ExpirationDate = model.ExpirationDate,
                MaxAccessCount = model.MaxAccessCount,
                Key = await _cryptoService.EncryptAsync(model.Key, key),
                Name = await _cryptoService.EncryptAsync(model.Name, model.CryptoKey),
                Notes = await _cryptoService.EncryptAsync(model.Notes, model.CryptoKey),
                HideEmail = model.HideEmail
            };
            EncByteArray encryptedFileData = null;

            if (password != null)
            {
                var passwordHash = await _cryptoFunctionService.Pbkdf2Async(password, model.Key,
                    CryptoHashAlgorithm.Sha256, 100000);
                send.Password = Convert.ToBase64String(passwordHash);
            }

            switch (send.Type)
            {
                case SendType.Text:
                    send.Text = new SendText
                    {
                        Text = await _cryptoService.EncryptAsync(model.Text.Text, model.CryptoKey),
                        Hidden = model.Text.Hidden
                    };
                    break;
                case SendType.File:
                    send.File = new SendFile();
                    if (fileData != null)
                    {
                        send.File.FileName = await _cryptoService.EncryptAsync(model.File.FileName, model.CryptoKey);
                        encryptedFileData = await _cryptoService.EncryptToBytesAsync(fileData, model.CryptoKey);
                    }
                    break;
                default:
                    break;
            }

            return (send, encryptedFileData);
        }

        public async Task<List<Send>> GetAllAsync()
        {
            var userId = await _userService.GetUserIdAsync();
            var sends = await _storageService.GetAsync<Dictionary<string, SendData>>(GetSendKey(userId));
            return sends?.Select(kvp => new Send(kvp.Value)).ToList() ?? new List<Send>();
        }

        public async Task<List<SendView>> GetAllDecryptedAsync()
        {
            if (_decryptedSendsCache != null)
            {
                return _decryptedSendsCache;
            }

            var hasKey = await _cryptoService.HasKeyAsync();
            if (!hasKey)
            {
                throw new Exception("No Key.");
            }

            if (_getAllDecryptedTask != null && !_getAllDecryptedTask.IsCompleted && !_getAllDecryptedTask.IsFaulted)
            {
                return await _getAllDecryptedTask;
            }

            async Task<List<SendView>> doTask()
            {
                var decSends = new List<SendView>();

                async Task decryptAndAddSendAsync(Send send) => decSends.Add(await send.DecryptAsync());
                await Task.WhenAll((await GetAllAsync()).Select(s => decryptAndAddSendAsync(s)));

                decSends = decSends.OrderBy(s => s, new SendLocaleComparer(_i18nService)).ToList();
                _decryptedSendsCache = decSends;
                return _decryptedSendsCache;
            }

            _getAllDecryptedTask = doTask();
            return await _getAllDecryptedTask;
        }

        public async Task<Send> GetAsync(string id)
        {
            var userId = await _userService.GetUserIdAsync();
            var sends = await _storageService.GetAsync<Dictionary<string, SendData>>(GetSendKey(userId));

            if (sends == null || !sends.ContainsKey(id))
            {
                return null;
            }

            return new Send(sends[id]);
        }

        public async Task ReplaceAsync(Dictionary<string, SendData> sends)
        {
            var userId = await _userService.GetUserIdAsync();
            await _storageService.SaveAsync(GetSendKey(userId), sends);
            _decryptedSendsCache = null;
        }

        public async Task<string> SaveWithServerAsync(Send send, EncByteArray encryptedFileData)
        {
            var request = new SendRequest(send, encryptedFileData?.Buffer?.LongLength);
            SendResponse response = default;
            if (send.Id == null)
            {
                switch (send.Type)
                {
                    case SendType.Text:
                        response = await _apiService.PostSendAsync(request);
                        break;
                    case SendType.File:
                        try{
                            var uploadDataResponse = await _apiService.PostFileTypeSendAsync(request);
                            response = uploadDataResponse.SendResponse;

                            await _fileUploadService.UploadSendFileAsync(uploadDataResponse, send.File.FileName, encryptedFileData);
                        }
                        catch (ApiException e) when (e.Error.StatusCode == HttpStatusCode.NotFound)
                        {
                            response = await LegacyServerSendFileUpload(request, send, encryptedFileData);
                        }
                        catch (Exception e) 
                        {
                            if (response != default){
                                await _apiService.DeleteSendAsync(response.Id);
                            }
                            throw e;
                        }
                        break;
                    default:
                        throw new NotImplementedException($"Cannot save unknown Send type {send.Type}");
                }
                send.Id = response.Id;
            }
            else
            {
                response = await _apiService.PutSendAsync(send.Id, request);
            }

            var userId = await _userService.GetUserIdAsync();
            await UpsertAsync(new SendData(response, userId));
            return response.Id;
        }

        [Obsolete("Mar 25 2021: This method has been deprecated in favor of direct uploads. This method still exists for backward compatibility with old server versions.")]
        private async Task<SendResponse> LegacyServerSendFileUpload(SendRequest request, Send send, EncByteArray encryptedFileData) {
            var fd = new MultipartFormDataContent($"--BWMobileFormBoundary{DateTime.UtcNow.Ticks}")
                        {
                            { new StringContent(JsonConvert.SerializeObject(request)), "model" },
                            { new ByteArrayContent(encryptedFileData.Buffer), "data", send.File.FileName.EncryptedString }
                        };

            return await _apiService.PostSendFileAsync(fd);
        }

        public async Task UpsertAsync(params SendData[] sends)
        {
            var userId = await _userService.GetUserIdAsync();
            var knownSends = await _storageService.GetAsync<Dictionary<string, SendData>>(GetSendKey(userId)) ??
                new Dictionary<string, SendData>();

            foreach (var send in sends)
            {
                knownSends[send.Id] = send;
            }

            await _storageService.SaveAsync(GetSendKey(userId), knownSends);
            _decryptedSendsCache = null;
        }

        public async Task RemovePasswordWithServerAsync(string id)
        {
            var response = await _apiService.PutSendRemovePasswordAsync(id);
            var userId = await _userService.GetUserIdAsync();
            await UpsertAsync(new SendData(response, userId));
        }

        private class SendLocaleComparer : IComparer<SendView>
        {
            private readonly II18nService _i18nService;

            public SendLocaleComparer(II18nService i18nService)
            {
                _i18nService = i18nService;
            }

            public int Compare(SendView a, SendView b)
            {
                var aName = a?.Name;
                var bName = b?.Name;
                if (aName == null && bName != null)
                {
                    return -1;
                }
                if (aName != null && bName == null)
                {
                    return 1;
                }
                if (aName == null && bName == null)
                {
                    return 0;
                }
                return _i18nService.StringComparer.Compare(aName, bName);
            }
        }
    }
}
