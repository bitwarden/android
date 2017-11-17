using static Android.App.Assist.AssistStructure;
using Android.App.Assist;

namespace Bit.Android.Autofill
{
    public class Parser
    {
        private readonly AssistStructure _structure;
        private string _uri;

        public Parser(AssistStructure structure)
        {
            _structure = structure;
        }

        public FieldCollection FieldCollection { get; private set; } = new FieldCollection();
        public FilledFieldCollection FilledFieldCollection { get; private set; } = new FilledFieldCollection();
        public string Uri
        {
            get => _uri;
            set
            {
                if(string.IsNullOrWhiteSpace(value))
                {
                    _uri = null;
                    return;
                }

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

        private void Parse(bool forFill)
        {
            for(var i = 0; i < _structure.WindowNodeCount; i++)
            {
                var node = _structure.GetWindowNodeAt(i);
                ParseNode(forFill, node.RootViewNode);
            }
        }

        private void ParseNode(bool forFill, ViewNode node)
        {
            var hints = node.GetAutofillHints();
            var isEditText = node.ClassName == "android.widget.EditText";
            if(isEditText || (hints?.Length ?? 0) > 0)
            {
                if(Uri == null)
                {
                    Uri = node.IdPackage;
                }

                if(forFill)
                {
                    FieldCollection.Add(new Field(node));
                }
                else
                {
                    FilledFieldCollection.Add(new FilledField(node));
                }
            }

            for(var i = 0; i < node.ChildCount; i++)
            {
                ParseNode(forFill, node.GetChildAt(i));
            }
        }
    }
}