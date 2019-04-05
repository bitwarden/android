using Android.Content;
using Android.Runtime;
using Android.Views;
using Bit.App.Controls.BoxedView;
using System;
using System.Linq;
using System.Linq.Expressions;
using System.Reflection;
using Xamarin.Forms.Platform.Android;

namespace Bit.Droid.Renderers
{
    [Preserve(AllMembers = true)]
    public class BaseCellRenderer<TNativeCell> : CellRenderer where TNativeCell : BaseCellView
    {
        internal static class InstanceCreator<T1, T2, TInstance>
        {
            public static Func<T1, T2, TInstance> Create { get; } = CreateInstance();

            private static Func<T1, T2, TInstance> CreateInstance()
            {
                var argsTypes = new[] { typeof(T1), typeof(T2) };
                var constructor = typeof(TInstance).GetConstructor(
                    BindingFlags.Public | BindingFlags.NonPublic | BindingFlags.Instance,
                    Type.DefaultBinder, argsTypes, null);
                var args = argsTypes.Select(Expression.Parameter).ToArray();
                return Expression.Lambda<Func<T1, T2, TInstance>>(Expression.New(constructor, args), args).Compile();
            }
        }

        protected override View GetCellCore(Xamarin.Forms.Cell item, View convertView, ViewGroup parent,
            Context context)
        {
            if(!(convertView is TNativeCell nativeCell))
            {
                nativeCell = InstanceCreator<Context, Xamarin.Forms.Cell, TNativeCell>.Create(context, item);
            }

            ClearPropertyChanged(nativeCell);
            nativeCell.Cell = item;
            SetUpPropertyChanged(nativeCell);
            nativeCell.UpdateCell();
            return nativeCell;
        }

        protected void SetUpPropertyChanged(BaseCellView nativeCell)
        {
            var formsCell = nativeCell.Cell as BaseCell;
            formsCell.PropertyChanged += nativeCell.CellPropertyChanged;
            if(formsCell.Parent is BoxedView parentElement)
            {
                parentElement.PropertyChanged += nativeCell.ParentPropertyChanged;
                var section = parentElement.Model.GetSection(BoxedModel.GetPath(formsCell).Item1);
                if(section != null)
                {
                    formsCell.Section = section;
                    formsCell.Section.PropertyChanged += nativeCell.SectionPropertyChanged;
                }
            }
        }

        private void ClearPropertyChanged(BaseCellView nativeCell)
        {
            var formsCell = nativeCell.Cell as BaseCell;
            formsCell.PropertyChanged -= nativeCell.CellPropertyChanged;
            if(formsCell.Parent is BoxedView parentElement)
            {
                parentElement.PropertyChanged -= nativeCell.ParentPropertyChanged;
                if(formsCell.Section != null)
                {
                    formsCell.Section.PropertyChanged -= nativeCell.SectionPropertyChanged;
                }
            }
        }
    }
}
