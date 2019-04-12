using System.Collections.Generic;
using System.Threading.Tasks;

namespace Bit.Core.Models.Domain
{
    public abstract class Domain
    {
        protected void BuildDomainModel<D, O>(D domain, O dataObj, HashSet<string> map, bool alreadyEncrypted,
            HashSet<string> notEncList = null)
            where D : Domain
            where O : Data.Data
        {
            var domainType = domain.GetType();
            var dataObjType = dataObj.GetType();
            foreach(var prop in map)
            {
                var dataObjPropInfo = dataObjType.GetProperty(prop);
                var dataObjProp = dataObjPropInfo.GetValue(dataObj);
                var domainPropInfo = domainType.GetProperty(prop);
                if(alreadyEncrypted || (notEncList?.Contains(prop) ?? false))
                {
                    domainPropInfo.SetValue(domain, dataObjProp, null);
                }
                else
                {
                    domainPropInfo.SetValue(domain,
                        dataObjProp != null ? new CipherString(dataObjProp as string) : null, null);
                }
            }
        }

        protected void BuildDataModel<D, O>(D domain, O dataObj, HashSet<string> map,
            HashSet<string> notCipherStringList = null)
            where D : Domain
            where O : Data.Data
        {
            var domainType = domain.GetType();
            var dataObjType = dataObj.GetType();
            foreach(var prop in map)
            {
                var domainPropInfo = domainType.GetProperty(prop);
                var domainProp = domainPropInfo.GetValue(domain);
                var dataObjPropInfo = dataObjType.GetProperty(prop);
                if(notCipherStringList?.Contains(prop) ?? false)
                {
                    dataObjPropInfo.SetValue(dataObj, domainProp, null);
                }
                else
                {
                    dataObjPropInfo.SetValue(dataObj, (domainProp as CipherString)?.EncryptedString, null);
                }
            }
        }

        protected async Task<V> DecryptObjAsync<V, D>(V viewModel, D domain, HashSet<string> map, string orgId)
            where V : View.View
        {
            var viewModelType = viewModel.GetType();
            var domainType = domain.GetType();

            Task<string> decCs(string propName)
            {
                var domainPropInfo = domainType.GetProperty(propName);
                var domainProp = domainPropInfo.GetValue(domain) as CipherString;
                if(domainProp != null)
                {
                    return domainProp.DecryptAsync(orgId);
                }
                return Task.FromResult((string)null);
            };
            void setDec(string propName, string val)
            {
                var viewModelPropInfo = viewModelType.GetProperty(propName);
                viewModelPropInfo.SetValue(viewModel, val, null);
            };

            var tasks = new List<Task>();
            foreach(var prop in map)
            {
                tasks.Add(decCs(prop).ContinueWith(async val => setDec(prop, await val)));
            }
            await Task.WhenAll(tasks);
            return viewModel;
        }
    }
}
