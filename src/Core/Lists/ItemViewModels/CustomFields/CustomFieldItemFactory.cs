using System;
using System.Windows.Input;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.View;

namespace Bit.App.Lists.ItemViewModels.CustomFields
{
    public interface ICustomFieldItemFactory
    {
        ICustomFieldItemViewModel CreateCustomFieldItem(FieldView field,
                                                        bool isEditing,
                                                        CipherView cipher,
                                                        IPasswordPromptable passwordPromptable,
                                                        ICommand copyFieldCommand,
                                                        ICommand fieldOptionsCommand);
    }

    public class CustomFieldItemFactory : ICustomFieldItemFactory
    {
        readonly II18nService _i18nService;
        readonly IEventService _eventService;

        public CustomFieldItemFactory(II18nService i18nService, IEventService eventService)
        {
            _i18nService = i18nService;
            _eventService = eventService;
        }

        public ICustomFieldItemViewModel CreateCustomFieldItem(FieldView field,
                                                               bool isEditing,
                                                               CipherView cipher,
                                                               IPasswordPromptable passwordPromptable,
                                                               ICommand copyFieldCommand,
                                                               ICommand fieldOptionsCommand)
        {
            switch (field.Type)
            {
                case FieldType.Text:
                    return new TextCustomFieldItemViewModel(field, isEditing, fieldOptionsCommand, copyFieldCommand);
                case FieldType.Boolean:
                    return new BooleanCustomFieldItemViewModel(field, isEditing, fieldOptionsCommand);
                case FieldType.Hidden:
                    return new HiddenCustomFieldItemViewModel(field, isEditing, fieldOptionsCommand, cipher, passwordPromptable, _eventService, copyFieldCommand);
                case FieldType.Linked:
                    return new LinkedCustomFieldItemViewModel(field, isEditing, fieldOptionsCommand, cipher, _i18nService);
                default:
                    throw new NotImplementedException("There is no custom field item for field type " + field.Type);
            }
        }
    }
}
