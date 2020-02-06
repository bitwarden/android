using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.View;
using System;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class ExportService : IExportService
    {
        private readonly ICryptoService _cryptoService;
        private readonly IUserService _userService;
        private readonly IApiService _apiService;
        private readonly IStorageService _storageService;
        private readonly II18nService _i18nService;
        private readonly ICipherService _cipherService;

        public ExportService(
            ICryptoService cryptoService,
            IUserService userService,
            IApiService apiService,
            IStorageService storageService,
            II18nService i18nService,
            ICipherService cipherService)
        {
            _cryptoService = cryptoService;
            _userService = userService;
            _apiService = apiService;
            _storageService = storageService;
            _i18nService = i18nService;
            _cipherService = cipherService;
        }

        public Task GetExport(string format = "csv")
        {
            throw new NotImplementedException();
            // TODO construct export file
        }

        public Task GetOrganizationExport(string organizationId, string format = "csv")
        {
            throw new NotImplementedException();
        }

        public string GetFileName(string prefix = null, string extension = "csv")
        {
            var dateString = DateTime.Now.ToString("yyyyMMddHHmmss");

            return "bitwarden" + (!string.IsNullOrEmpty(prefix) ? ("_" + prefix) : "") + "_export_" + dateString + "."
                   + extension;
        }
        
        private Cipher BuildCommonCipher(Cipher cipher, CipherView c)
        {
            throw new NotImplementedException();
            // TODO modify cipher values
        }
    }
}
