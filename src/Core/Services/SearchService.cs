using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class SearchService : ISearchService
    {
        private readonly ICipherService _cipherService;

        public SearchService(
            ICipherService cipherService)
        {
            _cipherService = cipherService;
        }

        public void ClearIndex()
        {
            // TODO
        }

        public bool IsSearchable(string query)
        {
            return true;
        }

        public Task IndexCiphersAsync()
        {
            // TODO
            return Task.FromResult(0);
        }

        public async Task<List<CipherView>> SearchCiphersAsync(string query, Func<CipherView, bool> filter = null,
            List<CipherView> ciphers = null)
        {
            var results = new List<CipherView>();
            if(query != null)
            {
                query = query.Trim().ToLower();
            }
            if(query == string.Empty)
            {
                query = null;
            }
            if(ciphers == null)
            {
                ciphers = await _cipherService.GetAllDecryptedAsync();
            }
            if(filter != null)
            {
                ciphers = ciphers.Where(filter).ToList();
            }
            if(!IsSearchable(query))
            {
                return ciphers;
            }

            return SearchCiphersBasic(ciphers, query);
            // TODO: advanced searching with index
        }

        public List<CipherView> SearchCiphersBasic(List<CipherView> ciphers, string query)
        {
            query = query.Trim().ToLower();
            return ciphers.Where(c =>
            {
                if(c.Name?.ToLower().Contains(query) ?? false)
                {
                    return true;
                }
                if(query.Length >= 8 && c.Id.StartsWith(query))
                {
                    return true;
                }
                if(c.SubTitle?.ToLower().Contains(query) ?? false)
                {
                    return true;
                }
                if(c.Login?.Uri?.ToLower()?.Contains(query) ?? false)
                {
                    return true;
                }
                return false;
            }).ToList();
        }
    }
}
