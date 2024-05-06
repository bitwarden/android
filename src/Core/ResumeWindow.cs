namespace Bit.Core
{
    // This ResumeWindow is used as a Workaround for Android to be able to find the current "IsActive" Window
    // It also allows setting a "PendingPage" on an existing Window which then navigates when the Window is active.
    public class ResumeWindow : Window
    {
        public Page PendingPage {get;set;}
        public bool IsActive { get; set; }

        public ResumeWindow(Page page) : base(page) { }

        /// <summary>
        /// You need to do this inside OnActivated not OnResumed
        /// Androids OnResume maps to OnActivated
        /// Androids OnRestart is what Maps to OnResumed
        /// I realize this is confusing from the perspective of Android
        /// https://github.com/dotnet/maui/issues/1720 explains it a bit better
        /// </summary>
        protected override void OnActivated()
        {
            base.OnActivated();

            if (PendingPage is not null)
                Page = PendingPage;

            PendingPage = null;
            IsActive = true;
        }

        protected override void OnDeactivated()
        {
            IsActive = false;
        }
    }
}
