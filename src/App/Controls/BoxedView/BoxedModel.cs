using System;
using System.Collections.Generic;
using System.Linq;
using Xamarin.Forms;
using Xamarin.Forms.Internals;

namespace Bit.App.Controls.BoxedView
{
    public class BoxedModel : TableModel
    {
        private static readonly BindableProperty PathProperty = BindableProperty.Create(
            "Path", typeof(Tuple<int, int>), typeof(Cell), null);

        private BoxedRoot _root;
        private IEnumerable<BoxedSection> _visibleSections;

        public BoxedModel(BoxedRoot root)
        {
            _root = root;
            _visibleSections = _root.Where(x => x.IsVisible);
        }

        public override Cell GetCell(int section, int row)
        {
            var cell = (Cell)GetItem(section, row);
            SetPath(cell, new Tuple<int, int>(section, row));
            return cell;
        }

        public override object GetItem(int section, int row)
        {
            return _visibleSections.ElementAt(section)[row];
        }

        public override int GetRowCount(int section)
        {
            return _visibleSections.ElementAt(section).Count;
        }

        public override int GetSectionCount()
        {
            return _visibleSections.Count();
        }

        public virtual BoxedSection GetSection(int section)
        {
            return _visibleSections.ElementAtOrDefault(section);
        }

        public override string GetSectionTitle(int section)
        {
            return _visibleSections.ElementAt(section).Title;
        }

        public virtual string GetFooterText(int section)
        {
            return _visibleSections.ElementAt(section).FooterText;
        }

        protected override void OnRowSelected(object item)
        {
            base.OnRowSelected(item);
            (item as BaseCell)?.OnTapped();
        }

        public virtual double GetHeaderHeight(int section)
        {
            return _visibleSections.ElementAt(section).HeaderHeight;
        }

        public static Tuple<int, int> GetPath(Cell item)
        {
            if(item == null)
            {
                throw new ArgumentNullException(nameof(item));
            }
            return item.GetValue(PathProperty) as Tuple<int, int>;
        }

        private static void SetPath(Cell item, Tuple<int, int> index)
        {
            item?.SetValue(PathProperty, index);
        }
    }
}
