using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class SearchService : ISearchService
    {
        private readonly ICipherService _cipherService;
        private readonly ISendService _sendService;

        public SearchService(
            ICipherService cipherService,
            ISendService sendService)
        {
            _cipherService = cipherService;
            _sendService = sendService;
        }

        public void ClearIndex()
        {
            // TODO
        }

        public bool IsSearchable(string query)
        {
            return (query?.Length ?? 0) > 1;
        }

        public Task IndexCiphersAsync()
        {
            // TODO
            return Task.FromResult(0);
        }

        public async Task<List<CipherView>> SearchCiphersAsync(string query, Func<CipherView, bool> filter = null,
            List<CipherView> ciphers = null, CancellationToken ct = default)
        {
            var results = new List<CipherView>();
            if (query != null)
            {
                query = query.Trim().ToLower();
            }
            if (query == string.Empty)
            {
                query = null;
            }
            if (ciphers == null)
            {
                ciphers = await _cipherService.GetAllDecryptedAsync();
            }

            ct.ThrowIfCancellationRequested();
            if (filter != null)
            {
                ciphers = ciphers.Where(filter).ToList();
            }

            ct.ThrowIfCancellationRequested();
            if (!IsSearchable(query))
            {
                return ciphers;
            }

            return SearchCiphersBasic(ciphers, query);
            // TODO: advanced searching with index
        }

        public List<CipherView> SearchCiphersBasic(List<CipherView> ciphers, string query,
            CancellationToken ct = default, bool deleted = false)
        {
            ct.ThrowIfCancellationRequested();
            query = query.Trim().ToLower();
            return ciphers.Where(c =>
            {
                ct.ThrowIfCancellationRequested();
                if (c.Name?.ToLower().Contains(query) ?? false)
                {
                    return true;
                }
                if (query.Length >= 8 && c.Id.StartsWith(query))
                {
                    return true;
                }
                if (c.SubTitle?.ToLower().Contains(query) ?? false)
                {
                    return true;
                }
                if (c.Login?.Uri?.ToLower()?.Contains(query) ?? false)
                {
                    return true;
                }
                return false;
            }).ToList();
        }

        public async Task<List<SendView>> SearchSendsAsync(string query, Func<SendView, bool> filter = null,
            List<SendView> sends = null, CancellationToken ct = default)
        {
            var results = new List<SendView>();
            if (query != null)
            {
                query = query.Trim().ToLower();
            }
            if (query == string.Empty)
            {
                query = null;
            }
            if (sends == null)
            {
                sends = await _sendService.GetAllDecryptedAsync();
            }

            ct.ThrowIfCancellationRequested();
            if (filter != null)
            {
                sends = sends.Where(filter).ToList();
            }

            ct.ThrowIfCancellationRequested();
            if (!IsSearchable(query))
            {
                return sends;
            }

            return SearchSendsBasic(sends, query);
        }

        public List<SendView> SearchSendsBasic(List<SendView> sends, string query, CancellationToken ct = default,
            bool deleted = false)
        {
            ct.ThrowIfCancellationRequested();
            query = query.Trim().ToLower();
            return sends.Where(s =>
            {
                ct.ThrowIfCancellationRequested();
                if (s.Name?.ToLower().Contains(query) ?? false)
                {
                    return true;
                }
                if (s.Text?.Text?.ToLower().Contains(query) ?? false)
                {
                    return true;
                }
                if (s.File?.FileName?.ToLower()?.Contains(query) ?? false)
                {
                    return true;
                }
                return false;
            }).ToList();
        }
    }
}
