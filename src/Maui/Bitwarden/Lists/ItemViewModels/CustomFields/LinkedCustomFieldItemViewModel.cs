using System.Collections.Generic;
using System.Linq;
using System.Windows.Input;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.View;

namespace Bit.App.Lists.ItemViewModels.CustomFields
{
    public class LinkedCustomFieldItemViewModel : BaseCustomFieldItemViewModel
    {
        private readonly CipherView _cipher;
        private readonly II18nService _i18nService;
        private int _linkedFieldOptionSelectedIndex;

        public LinkedCustomFieldItemViewModel(FieldView field, bool isEditing, ICommand fieldOptionsCommand, CipherView cipher, II18nService i18nService)
            : base(field, isEditing, fieldOptionsCommand)
        {
            _cipher = cipher;
            _i18nService = i18nService;

            LinkedFieldOptionSelectedIndex = Field.LinkedId.HasValue
                ? LinkedFieldOptions.FindIndex(lfo => lfo.Value == Field.LinkedId.Value)
                : 0;

            if (isEditing && Field.LinkedId is null)
            {
                field.LinkedId = LinkedFieldOptions[0].Value;
            }
        }

        public override string ValueText
        {
            get
            {
                var i18nKey = _cipher.LinkedFieldI18nKey(Field.LinkedId.GetValueOrDefault());
                return $"{BitwardenIcons.Link} {_i18nService.T(i18nKey)}";
            }
        }

        public int LinkedFieldOptionSelectedIndex
        {
            get => _linkedFieldOptionSelectedIndex;
            set
            {
                if (SetProperty(ref _linkedFieldOptionSelectedIndex, value))
                {
                    LinkedFieldValueChanged();
                }
            }
        }

        public List<KeyValuePair<string, LinkedIdType>> LinkedFieldOptions
        {
            get => _cipher.LinkedFieldOptions
                .Select(kvp => new KeyValuePair<string, LinkedIdType>(_i18nService.T(kvp.Key), kvp.Value))
                .ToList();
        }

        private void LinkedFieldValueChanged()
        {
            if (Field != null && LinkedFieldOptionSelectedIndex > -1)
            {
                Field.LinkedId = LinkedFieldOptions.Find(lfo =>
                    lfo.Value == LinkedFieldOptions[LinkedFieldOptionSelectedIndex].Value).Value;
            }
        }
    }
}
