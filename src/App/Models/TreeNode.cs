using Bit.App.Abstractions;
using System.Collections.Generic;

namespace Bit.App.Models
{
    public class TreeNode<T> where T : ITreeNodeObject
    {
        public TreeNode(T node, string name, T parent)
        {
            Parent = parent;
            Node = node;
            Node.Name = name;
        }

        public T Parent { get; set; }
        public T Node { get; set; }
        public List<TreeNode<T>> Children { get; set; } = new List<TreeNode<T>>();
    }
}
