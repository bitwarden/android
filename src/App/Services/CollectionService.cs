using System;
using System.Linq;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models;

namespace Bit.App.Services
{
    public class CollectionService : ICollectionService
    {
        private readonly ICollectionRepository _collectionRepository;
        private readonly ICipherCollectionRepository _cipherCollectionRepository;
        private readonly IAuthService _authService;

        public CollectionService(
            ICollectionRepository collectionRepository,
            ICipherCollectionRepository cipherCollectionRepository,
            IAuthService authService)
        {
            _collectionRepository = collectionRepository;
            _cipherCollectionRepository = cipherCollectionRepository;
            _authService = authService;
        }

        public async Task<Collection> GetByIdAsync(string id)
        {
            var data = await _collectionRepository.GetByIdAsync(id);
            if(data == null || data.UserId != _authService.UserId)
            {
                return null;
            }

            var collection = new Collection(data);
            return collection;
        }

        public async Task<IEnumerable<Collection>> GetAllAsync()
        {
            var data = await _collectionRepository.GetAllByUserIdAsync(_authService.UserId);
            var collections = data.Select(c => new Collection(c));
            return collections;
        }

        public async Task<IEnumerable<Tuple<string, string>>> GetAllCipherAssociationsAsync()
        {
            var data = await _cipherCollectionRepository.GetAllByUserIdAsync(_authService.UserId);
            var assocs = data.Select(cc => new Tuple<string, string>(cc.CipherId, cc.CollectionId));
            return assocs;
        }
    }
}
