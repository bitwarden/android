using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

using Android.App;
using Android.Content;
using Android.OS;
using Android.Runtime;
using Android.Service.Autofill;
using Android.Views;
using Android.Widget;
using Android.Views.Autofill;
using static Android.App.Assist.AssistStructure;
using Android.Text;
using Android.App.Assist;

namespace Bit.Android
{
    [Service(Permission = global::Android.Manifest.Permission.BindAutofillService, Label = "bitwarden")]
    [IntentFilter(new string[] { "android.service.autofill.AutofillService" })]
    [MetaData("android.autofill", Resource = "@xml/autofillservice")]
    public class AutofillFrameworkService : global::Android.Service.Autofill.AutofillService
    {
        public override void OnFillRequest(FillRequest request, CancellationSignal cancellationSignal,
            FillCallback callback)
        {
            var structure = request.FillContexts?.LastOrDefault()?.Structure;
            if(structure == null)
            {
                return;
            }

            var clientState = request.ClientState;

            var parser = new StructureParser(structure);
            parser.ParseForFill();

            // build response
            var responseBuilder = new FillResponse.Builder();

            var username1 = new FilledAutofillField { TextValue = "username1" };
            var password1 = new FilledAutofillField { TextValue = "pass1" };
            var login1 = new Dictionary<string, FilledAutofillField>
            {
                { View.AutofillHintUsername, username1 },
                { View.AutofillHintPassword, password1 }
            };
            var coll = new FilledAutofillFieldCollection("Login 1 Name", login1);

            var username2 = new FilledAutofillField { TextValue = "username2" };
            var password2 = new FilledAutofillField { TextValue = "pass2" };
            var login2 = new Dictionary<string, FilledAutofillField>
            {
                { View.AutofillHintUsername, username2 },
                { View.AutofillHintPassword, password2 }
            };
            var col2 = new FilledAutofillFieldCollection("Login 2 Name", login2);

            var clientFormDataMap = new Dictionary<string, FilledAutofillFieldCollection>
            {
                { "login-1-guid", coll },
                { "login-2-guid", col2 }
            };

            var response = AutofillHelper.NewResponse(this, false, parser.AutofillFields, clientFormDataMap);
            // end build response

            callback.OnSuccess(response);
        }

        public override void OnSaveRequest(SaveRequest request, SaveCallback callback)
        {
            var structure = request.FillContexts?.LastOrDefault()?.Structure;
            if(structure == null)
            {
                return;
            }

            var clientState = request.ClientState;

            var parser = new StructureParser(structure);
            parser.ParseForSave();
            var filledAutofillFieldCollection = parser.GetClientFormData();
            //SaveFilledAutofillFieldCollection(filledAutofillFieldCollection);
        }
    }

    /////////////////// Helper Classes ///////////////////////

    public class StructureParser
    {
        private readonly AssistStructure _structure;
        private FilledAutofillFieldCollection _filledAutofillFieldCollection;

        public StructureParser(AssistStructure structure)
        {
            _structure = structure;
        }

        public AutofillFieldMetadataCollection AutofillFields { get; private set; }
            = new AutofillFieldMetadataCollection();

        public void ParseForFill()
        {
            Parse(true);
        }

        public void ParseForSave()
        {
            Parse(false);
        }

        /**
         * Traverse AssistStructure and add ViewNode metadata to a flat list.
         */
        private void Parse(bool forFill)
        {
            _filledAutofillFieldCollection = new FilledAutofillFieldCollection();

            for(var i = 0; i < _structure.WindowNodeCount; i++)
            {
                var node = _structure.GetWindowNodeAt(i);
                var view = node.RootViewNode;
                ParseLocked(forFill, view);
            }
        }

        private void ParseLocked(bool forFill, ViewNode viewNode)
        {
            var autofillHints = viewNode.GetAutofillHints();
            var autofillType = (AutofillType)(int)viewNode.AutofillType;
            var inputType = (InputTypes)(int)viewNode.InputType;
            var isEditText = viewNode.ClassName == "android.widget.EditText";
            if(isEditText || (autofillHints?.Length ?? 0) > 0)
            {
                if(forFill)
                {
                    AutofillFields.Add(new AutofillFieldMetadata(viewNode));
                }
                else
                {
                    _filledAutofillFieldCollection.Add(new FilledAutofillField(viewNode));
                }
            }

            for(var i = 0; i < viewNode.ChildCount; i++)
            {
                ParseLocked(forFill, viewNode.GetChildAt(i));
            }
        }

        public FilledAutofillFieldCollection GetClientFormData()
        {
            return _filledAutofillFieldCollection;
        }
    }

    public class AutofillFieldMetadataCollection
    {
        private int _size = 0;

        public List<int> Ids { get; private set; } = new List<int>();
        public List<AutofillId> AutofillIds { get; private set; } = new List<AutofillId>();
        public SaveDataType SaveType { get; private set; } = SaveDataType.Generic;
        public List<string> AutofillHints { get; private set; } = new List<string>();
        public List<string> FocusedAutofillHints { get; private set; } = new List<string>();
        public List<AutofillFieldMetadata> Feilds { get; private set; }
        public IDictionary<int, AutofillFieldMetadata> IdToFieldMap { get; private set; } =
            new Dictionary<int, AutofillFieldMetadata>();
        public IDictionary<string, List<AutofillFieldMetadata>> AutofillHintsToFieldsMap { get; private set; } =
            new Dictionary<string, List<AutofillFieldMetadata>>();

        public void Add(AutofillFieldMetadata data)
        {
            _size++;
            SaveType |= data.SaveType;
            Ids.Add(data.Id);
            AutofillIds.Add(data.AutofillId);
            IdToFieldMap.Add(data.Id, data);

            if((data.AutofillHints?.Count ?? 0) > 0)
            {
                AutofillHints.AddRange(data.AutofillHints);
                if(data.IsFocused)
                {
                    FocusedAutofillHints.AddRange(data.AutofillHints);
                }

                foreach(var hint in data.AutofillHints)
                {
                    if(!AutofillHintsToFieldsMap.ContainsKey(hint))
                    {
                        AutofillHintsToFieldsMap.Add(hint, new List<AutofillFieldMetadata>());
                    }

                    AutofillHintsToFieldsMap[hint].Add(data);
                }
            }
        }
    }

    public class AutofillFieldMetadata
    {
        private List<string> _autofillHints;
        private string[] _autofillOptions;

        public AutofillFieldMetadata(ViewNode view)
        {
            _autofillOptions = view.GetAutofillOptions();
            Id = view.Id;
            AutofillId = view.AutofillId;
            AutofillType = (AutofillType)(int)view.AutofillType;
            InputType = (InputTypes)(int)view.InputType;
            IsFocused = view.IsFocused;
            AutofillHints = AutofillHelper.FilterForSupportedHints(view.GetAutofillHints())?.ToList() ?? new List<string>();
        }

        public SaveDataType SaveType { get; set; } = SaveDataType.Generic;
        public List<string> AutofillHints
        {
            get { return _autofillHints; }
            set
            {
                _autofillHints = value;
                UpdateSaveTypeFromHints();
            }
        }
        public int Id { get; private set; }
        public AutofillId AutofillId { get; private set; }
        public AutofillType AutofillType { get; private set; }
        public InputTypes InputType { get; private set; }
        public bool IsFocused { get; private set; }

        /**
         * When the {@link ViewNode} is a list that the user needs to choose a string from (i.e. a
         * spinner), this is called to return the index of a specific item in the list.
         */
        public int GetAutofillOptionIndex(string value)
        {
            for(var i = 0; i < _autofillOptions.Length; i++)
            {
                if(_autofillOptions[i].Equals(value))
                {
                    return i;
                }
            }

            return -1;
        }

        private void UpdateSaveTypeFromHints()
        {
            SaveType = SaveDataType.Generic;
            if(_autofillHints == null)
            {
                return;
            }

            foreach(var hint in _autofillHints)
            {
                switch(hint)
                {
                    case View.AutofillHintCreditCardExpirationDate:
                    case View.AutofillHintCreditCardExpirationDay:
                    case View.AutofillHintCreditCardExpirationMonth:
                    case View.AutofillHintCreditCardExpirationYear:
                    case View.AutofillHintCreditCardNumber:
                    case View.AutofillHintCreditCardSecurityCode:
                        SaveType |= SaveDataType.CreditCard;
                        break;
                    case View.AutofillHintEmailAddress:
                        SaveType |= SaveDataType.EmailAddress;
                        break;
                    case View.AutofillHintPhone:
                    case View.AutofillHintName:
                        SaveType |= SaveDataType.Generic;
                        break;
                    case View.AutofillHintPassword:
                        SaveType |= SaveDataType.Password;
                        SaveType &= ~SaveDataType.EmailAddress;
                        SaveType &= ~SaveDataType.Username;
                        break;
                    case View.AutofillHintPostalAddress:
                    case View.AutofillHintPostalCode:
                        SaveType |= SaveDataType.Address;
                        break;
                    case View.AutofillHintUsername:
                        SaveType |= SaveDataType.Username;
                        break;
                }
            }
        }
    }

    public class FilledAutofillField
    {
        /**
         * Does not need to be serialized into persistent storage, so it's not exposed.
         */
        private string[] _autofillHints = null;

        public FilledAutofillField() { }

        public FilledAutofillField(ViewNode viewNode)
        {
            _autofillHints = AutofillHelper.FilterForSupportedHints(viewNode.GetAutofillHints());
            var autofillValue = viewNode.AutofillValue;
            if(autofillValue != null)
            {
                if(autofillValue.IsList)
                {
                    var autofillOptions = viewNode.GetAutofillOptions();
                    int index = autofillValue.ListValue;
                    if(autofillOptions != null && autofillOptions.Length > 0)
                    {
                        TextValue = autofillOptions[index];
                    }
                }
                else if(autofillValue.IsDate)
                {
                    DateValue = autofillValue.DateValue;
                }
                else if(autofillValue.IsText)
                {
                    // Using toString of AutofillValue.getTextValue in order to save it to
                    // SharedPreferences.
                    TextValue = autofillValue.TextValue;
                }
            }
        }

        public string TextValue { get; set; }
        public long? DateValue { get; set; }
        public bool? ToggleValue { get; set; }

        public string[] GetAutofillHints()
        {
            return _autofillHints;
        }

        public bool IsNull()
        {
            return TextValue == null && DateValue == null && ToggleValue == null;
        }

        public override bool Equals(object o)
        {
            if(this == o)
                return true;

            if(o == null || GetType() != o.GetType())
                return false;

            var that = (FilledAutofillField)o;
            if(TextValue != null ? !TextValue.Equals(that.TextValue) : that.TextValue != null)
                return false;
            if(DateValue != null ? !DateValue.Equals(that.DateValue) : that.DateValue != null)
                return false;

            return ToggleValue != null ? ToggleValue.Equals(that.ToggleValue) : that.ToggleValue == null;
        }

        public override int GetHashCode()
        {
            var result = TextValue != null ? TextValue.GetHashCode() : 0;
            result = 31 * result + (DateValue != null ? DateValue.GetHashCode() : 0);
            result = 31 * result + (ToggleValue != null ? ToggleValue.GetHashCode() : 0);
            return result;
        }
    }

    public class FilledAutofillFieldCollection
    {
        public FilledAutofillFieldCollection()
            : this(null, new Dictionary<string, FilledAutofillField>())
        {
        }

        public FilledAutofillFieldCollection(string datasetName, IDictionary<string, FilledAutofillField> hintMap)
        {
            HintMap = hintMap;
            DatasetName = datasetName;
        }

        public IDictionary<string, FilledAutofillField> HintMap { get; private set; }
        public string DatasetName { get; set; }

        /**
         * Adds a {@code FilledAutofillField} to the collection, indexed by all of its hints.
         */
        public void Add(FilledAutofillField filledAutofillField)
        {
            if(filledAutofillField == null)
            {
                throw new ArgumentNullException(nameof(filledAutofillField));
            }

            var autofillHints = filledAutofillField.GetAutofillHints();
            foreach(var hint in autofillHints)
            {
                HintMap.Add(hint, filledAutofillField);
            }
        }

        /**
         * Populates a {@link Dataset.Builder} with appropriate values for each {@link AutofillId}
         * in a {@code AutofillFieldMetadataCollection}.
         *
         * In other words, it constructs an autofill
         * {@link Dataset.Builder} by applying saved values (from this {@code FilledAutofillFieldCollection})
         * to Views specified in a {@code AutofillFieldMetadataCollection}, which represents the current
         * page the user is on.
         */
        public bool ApplyToFields(AutofillFieldMetadataCollection autofillFieldMetadataCollection,
            Dataset.Builder datasetBuilder)
        {
            var setValueAtLeastOnce = false;
            var allHints = autofillFieldMetadataCollection.AutofillHints;
            for(var hintIndex = 0; hintIndex < allHints.Count; hintIndex++)
            {
                var hint = allHints[hintIndex];
                if(!autofillFieldMetadataCollection.AutofillHintsToFieldsMap.ContainsKey(hint))
                {
                    continue;
                }

                var fillableAutofillFields = autofillFieldMetadataCollection.AutofillHintsToFieldsMap[hint];
                for(var autofillFieldIndex = 0; autofillFieldIndex < fillableAutofillFields.Count; autofillFieldIndex++)
                {
                    if(!HintMap.ContainsKey(hint))
                    {
                        continue;
                    }

                    var filledAutofillField = HintMap[hint];
                    var autofillFieldMetadata = fillableAutofillFields[autofillFieldIndex];
                    var autofillId = autofillFieldMetadata.AutofillId;
                    var autofillType = autofillFieldMetadata.AutofillType;
                    switch(autofillType)
                    {
                        case AutofillType.List:
                            int listValue = autofillFieldMetadata.GetAutofillOptionIndex(filledAutofillField.TextValue);
                            if(listValue != -1)
                            {
                                datasetBuilder.SetValue(autofillId, AutofillValue.ForList(listValue));
                                setValueAtLeastOnce = true;
                            }
                            break;
                        case AutofillType.Date:
                            var dateValue = filledAutofillField.DateValue;
                            if(dateValue != null)
                            {
                                datasetBuilder.SetValue(autofillId, AutofillValue.ForDate(dateValue.Value));
                                setValueAtLeastOnce = true;
                            }
                            break;
                        case AutofillType.Text:
                            var textValue = filledAutofillField.TextValue;
                            if(textValue != null)
                            {
                                datasetBuilder.SetValue(autofillId, AutofillValue.ForText(textValue));
                                setValueAtLeastOnce = true;
                            }
                            break;
                        case AutofillType.Toggle:
                            var toggleValue = filledAutofillField.ToggleValue;
                            if(toggleValue != null)
                            {
                                datasetBuilder.SetValue(autofillId, AutofillValue.ForToggle(toggleValue.Value));
                                setValueAtLeastOnce = true;
                            }
                            break;
                        case AutofillType.None:
                        default:
                            break;
                    }
                }
            }

            return setValueAtLeastOnce;
        }

        /**
         * Takes in a list of autofill hints (`autofillHints`), usually associated with a View or set of
         * Views. Returns whether any of the filled fields on the page have at least 1 of these
         * `autofillHint`s.
         */
        public bool HelpsWithHints(List<String> autofillHints)
        {
            for(var i = 0; i < autofillHints.Count; i++)
            {
                if(HintMap.ContainsKey(autofillHints[i]) && !HintMap[autofillHints[i]].IsNull())
                {
                    return true;
                }
            }

            return false;
        }
    }

    public static class AutofillHelper
    {
        /**
         * Wraps autofill data in a LoginCredential  Dataset object which can then be sent back to the
         * client View.
         */
        public static Dataset NewDataset(Context context, AutofillFieldMetadataCollection autofillFields,
            FilledAutofillFieldCollection filledAutofillFieldCollection, bool datasetAuth)
        {
            var datasetName = filledAutofillFieldCollection.DatasetName;
            if(datasetName != null)
            {
                Dataset.Builder datasetBuilder;
                if(datasetAuth)
                {
                    datasetBuilder = new Dataset.Builder(
                        NewRemoteViews(context.PackageName, datasetName, "username", Resource.Drawable.fa_lock));
                    //IntentSender sender = AuthActivity.getAuthIntentSenderForDataset(context, datasetName);
                    //datasetBuilder.SetAuthentication(sender);
                }
                else
                {
                    datasetBuilder = new Dataset.Builder(
                        NewRemoteViews(context.PackageName, datasetName, "username", Resource.Drawable.user));
                }

                var setValueAtLeastOnce = filledAutofillFieldCollection.ApplyToFields(autofillFields, datasetBuilder);
                if(setValueAtLeastOnce)
                {
                    return datasetBuilder.Build();
                }
            }

            return null;
        }

        public static RemoteViews NewRemoteViews(string packageName, string text, string subtext, int iconId)
        {
            var views = new RemoteViews(packageName, Resource.Layout.autofill_listitem);
            views.SetTextViewText(Resource.Id.text, text);
            views.SetTextViewText(Resource.Id.text2, subtext);
            views.SetImageViewResource(Resource.Id.icon, iconId);
            return views;
        }

        /**
         * Wraps autofill data in a Response object (essentially a series of Datasets) which can then
         * be sent back to the client View.
         */
        public static FillResponse NewResponse(Context context, bool datasetAuth,
            AutofillFieldMetadataCollection autofillFields,
            IDictionary<string, FilledAutofillFieldCollection> clientFormDataMap)
        {
            var responseBuilder = new FillResponse.Builder();
            if(clientFormDataMap != null)
            {
                foreach(var datasetName in clientFormDataMap.Keys)
                {
                    if(clientFormDataMap.ContainsKey(datasetName))
                    {
                        var dataset = NewDataset(context, autofillFields, clientFormDataMap[datasetName], datasetAuth);
                        if(dataset != null)
                        {
                            responseBuilder.AddDataset(dataset);
                        }
                    }
                }
            }

            if(autofillFields.SaveType != SaveDataType.Generic)
            {
                responseBuilder.SetSaveInfo(
                    new SaveInfo.Builder(autofillFields.SaveType, autofillFields.AutofillIds.ToArray()).Build());
                return responseBuilder.Build();
            }
            else
            {
                //Log.d(TAG, "These fields are not meant to be saved by autofill.");
                return null;
            }
        }

        public static string[] FilterForSupportedHints(string[] hints)
        {
            if((hints?.Length ?? 0) == 0)
            {
                return new string[0];
            }

            var filteredHints = new string[hints.Length];
            var i = 0;
            foreach(var hint in hints)
            {
                if(IsValidHint(hint))
                {
                    filteredHints[i++] = hint;
                }
            }

            var finalFilteredHints = new string[i];
            Array.Copy(filteredHints, 0, finalFilteredHints, 0, i);
            return finalFilteredHints;
        }

        public static bool IsValidHint(string hint)
        {
            switch(hint)
            {
                case View.AutofillHintCreditCardExpirationDate:
                case View.AutofillHintCreditCardExpirationDay:
                case View.AutofillHintCreditCardExpirationMonth:
                case View.AutofillHintCreditCardExpirationYear:
                case View.AutofillHintCreditCardNumber:
                case View.AutofillHintCreditCardSecurityCode:
                case View.AutofillHintEmailAddress:
                case View.AutofillHintPhone:
                case View.AutofillHintName:
                case View.AutofillHintPassword:
                case View.AutofillHintPostalAddress:
                case View.AutofillHintPostalCode:
                case View.AutofillHintUsername:
                    return true;
                default:
                    return false;
            }
        }
    }
}
