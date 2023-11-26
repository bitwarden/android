using Bit.App.Controls;

namespace Bit.App.Controls
{
    // WORKAROUND: There is an issue causing Multiline Labels that also have a LineBreakMode to not display text properly. (it truncates text on first line even with space available)
    // MAUI Github Issue: https://github.com/dotnet/maui/issues/14125 and https://github.com/dotnet/maui/pull/14918
    // This class is used to be able to only add the workaround to this specific Label avoiding potential issues on other "normal" Label
    // When this gets fixed by MAUI we can remove this class and some of the Mapping in LabelHandlerMappings
    public class TruncatedMultilineCustomLabel : CustomLabel
    {
    }
}
