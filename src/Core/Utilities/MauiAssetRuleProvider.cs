using Nager.PublicSuffix.Extensions;
using Nager.PublicSuffix.Models;
using Nager.PublicSuffix.RuleParsers;
using Nager.PublicSuffix.RuleProviders;

namespace Bit.Core.Utilities;

public class MauiAssetRuleProvider : BaseRuleProvider
{
    private readonly string _fileName;

    public MauiAssetRuleProvider(string fileName = "public_suffix_list.dat") => _fileName = fileName;

    public override async Task<bool> BuildAsync(bool ignoreCache = false, CancellationToken cancellationToken = default)
    {
        var rules = new TldRuleParser().ParseRules(await LoadFromMauiAssetAsync().ConfigureAwait(false)).ToArray();
        new DomainDataStructure("*", new TldRule("*")).AddRules(rules);
        CreateDomainDataStructure(rules);
        return true;
    }

    private async Task<string> LoadFromMauiAssetAsync()
    {
        try
        {
            await using var stream = await FileSystem.OpenAppPackageFileAsync(_fileName);
            using var reader = new StreamReader(stream);

            return await reader.ReadToEndAsync().ConfigureAwait(false);
        }
        catch
        {
            return null;
        }
    }
}
