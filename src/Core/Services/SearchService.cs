using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;

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
            var matchedCiphers = new List<CipherView>();
            var lowPriorityMatchedCiphers = new List<CipherView>();
            query = query.Trim().ToLower().RemoveDiacritics();

            foreach (var c in ciphers)
            {
                ct.ThrowIfCancellationRequested();
                if (c.Name?.ToLower().RemoveDiacritics().Contains(query) ?? false)
                {
                    matchedCiphers.Add(c);
                }
                else if (query.Length >= 8 && c.Id.StartsWith(query))
                {
                    lowPriorityMatchedCiphers.Add(c);
                }
                else if (c.SubTitle?.ToLower().RemoveDiacritics().Contains(query) ?? false)
                {
                    lowPriorityMatchedCiphers.Add(c);
                }
                else if (c.Login?.Uri?.ToLower()?.Contains(query) ?? false)
                {
                    lowPriorityMatchedCiphers.Add(c);
                }
            }

            ct.ThrowIfCancellationRequested();
            matchedCiphers.AddRange(lowPriorityMatchedCiphers);
            return matchedCiphers;
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
            var matchedSends = new List<SendView>();
            var lowPriorityMatchSends = new List<SendView>();
            ct.ThrowIfCancellationRequested();
            query = query.Trim().ToLower();

            foreach (var s in sends)
            {
                ct.ThrowIfCancellationRequested();
                if (s.Name?.ToLower().Contains(query) ?? false)
                {
                    matchedSends.Add(s);
                }
                else if (s.Text?.Text?.ToLower().Contains(query) ?? false)
                {
                    lowPriorityMatchSends.Add(s);
                }
                else if (s.File?.FileName?.ToLower()?.Contains(query) ?? false)
                {
                    lowPriorityMatchSends.Add(s);
                }
            }

            ct.ThrowIfCancellationRequested();
            matchedSends.AddRange(lowPriorityMatchSends);
            return matchedSends;
        }
    }
}
