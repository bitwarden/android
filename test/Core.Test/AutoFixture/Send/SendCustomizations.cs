using AutoFixture;
using Bit.Core.Enums;
using Bit.Core.Models.Api;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;
using Bit.Core.Models.Response;
using Bit.Core.Models.View;

namespace Bit.Core.Test.AutoFixture
{
    internal class TextSendCustomization : ICustomization
    {
        public void Customize(IFixture fixture)
        {
            fixture.Customize<SendData>(composer => composer
                .With(c => c.Type, SendType.Text)
                .With(c => c.Text, fixture.Create<SendTextData>())
                .Without(c => c.File));
            fixture.Customize<Send>(composer => composer
                .With(c => c.Type, SendType.Text)
                .With(c => c.Text, fixture.Create<SendText>())
                .Without(c => c.File));
            fixture.Customize<SendView>(composer => composer
                .With(c => c.Type, SendType.Text)
                .With(c => c.Text, fixture.Create<SendTextView>())
                .Without(c => c.File));
            fixture.Customize<SendRequest>(composer => composer
                .With(c => c.Type, SendType.Text)
                .With(c => c.Text, fixture.Create<SendTextApi>())
                .Without(c => c.File));
            fixture.Customize<SendResponse>(composer => composer
                .With(c => c.Type, SendType.Text)
                .With(c => c.Text, fixture.Create<SendTextApi>())
                .Without(c => c.File));
        }
    }

    internal class FileSendCustomization : ICustomization
    {
        public void Customize(IFixture fixture)
        {
            fixture.Customize<SendData>(composer => composer
                .With(c => c.Type, SendType.File)
                .With(c => c.File, fixture.Create<SendFileData>())
                .Without(c => c.Text));
            fixture.Customize<Send>(composer => composer
                .With(c => c.Type, SendType.File)
                .With(c => c.File, fixture.Create<SendFile>())
                .Without(c => c.Text));
            fixture.Customize<SendView>(composer => composer
                .With(c => c.Type, SendType.File)
                .With(c => c.File, fixture.Create<SendFileView>())
                .Without(c => c.Text));
            fixture.Customize<SendRequest>(composer => composer
                .With(c => c.Type, SendType.File)
                .With(c => c.File, fixture.Create<SendFileApi>())
                .Without(c => c.Text));
            fixture.Customize<SendResponse>(composer => composer
                .With(c => c.Type, SendType.File)
                .With(c => c.File, fixture.Create<SendFileApi>())
                .Without(c => c.Text));
        }
    }
}
