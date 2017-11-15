using static Android.App.Assist.AssistStructure;
using Android.App.Assist;

namespace Bit.Android.Autofill
{
    public class Parser
    {
        private readonly AssistStructure _structure;
        private string _uri;
        private FilledFieldCollection _filledAutofillFieldCollection;

        public Parser(AssistStructure structure)
        {
            _structure = structure;
        }

        public FieldCollection FieldCollection { get; private set; } = new FieldCollection();
        public string Uri
        {
            get => _uri;
            set
            {
                _uri = $"androidapp://{value}";
            }
        }

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
            _filledAutofillFieldCollection = new FilledFieldCollection();

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
            var autofillType = viewNode.AutofillType;
            var inputType = viewNode.InputType;
            var isEditText = viewNode.ClassName == "android.widget.EditText";
            if(isEditText || (autofillHints?.Length ?? 0) > 0)
            {
                if(forFill)
                {
                    var f = new Field(viewNode);
                    FieldCollection.Add(f);

                    if(Uri == null)
                    {
                        Uri = viewNode.IdPackage;
                    }
                }
                else
                {
                    _filledAutofillFieldCollection.Add(new FilledField(viewNode));
                }
            }

            for(var i = 0; i < viewNode.ChildCount; i++)
            {
                ParseLocked(forFill, viewNode.GetChildAt(i));
            }
        }

        public FilledFieldCollection GetClientFormData()
        {
            return _filledAutofillFieldCollection;
        }
    }
}