using Bit.UITests.Helpers;
using NUnit.Framework;
using NUnit.Framework.Interfaces;
using Xamarin.UITest;

namespace Bit.UITests.Setup
{

    [TestFixture(Platform.Android)]
    [TestFixture(Platform.iOS)]
    public abstract class BaseTestFixture
    {
        protected IApp App => AppManager.App;

        protected bool OnAndroid => AppManager.Platform == Platform.Android;

        protected bool OniOS => AppManager.Platform == Platform.iOS;

        protected BaseTestFixture(Platform platform)
        {
            AppManager.Platform = platform;
        }

        [SetUp]
        public virtual void BeforeEachTest()
        {
            AppManager.StartApp();
        }

        [TearDown]
        public void TearDown()
        {
            if (TestContext.CurrentContext.Result.Outcome.Status != TestStatus.Failed)
            {
                return;
            }

            if (App == null)
            {
                return;
            }

            if (TestEnvironment.Platform != TestPlatform.Local)
            {
                return;
            }

            // NOTE uncomment to help debug failing tests
            //App.Repl();
        }
    }
}
