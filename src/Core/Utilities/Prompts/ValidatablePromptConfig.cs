using System;

namespace Bit.App.Utilities.Prompts
{
    public class ValidatablePromptConfig
    {
        public string Title { get; set; }
        public string Subtitle { get; set; }
        public string Text { get; set; }
        public Func<string, string> ValidateText { get; set; }
        public string ValueSubInfo { get; set; }
        public string OkButtonText { get; set; }
        public string CancelButtonText { get; set; }
        public string ThirdButtonText { get; set; }
        public bool NumericKeyboard { get; set; }
    }

    public struct ValidatablePromptResponse
    {
        public ValidatablePromptResponse(string text, bool executeThirdAction)
        {
            Text = text;
            ExecuteThirdAction = executeThirdAction;
        }

        public string Text { get; set; }
        public bool ExecuteThirdAction { get; set; }
    }
}
