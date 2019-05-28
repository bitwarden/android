using System.Collections.Generic;

namespace Bit.Core.Models.Domain
{
    public class TreeNode<T> where T : ITreeNodeObject
    {
        public T Parent { get; set; }
        public T Node { get; set; }
        public List<TreeNode<T>> Children { get; set; } = new List<TreeNode<T>>();

        public TreeNode(T node, string name, T parent)
        {
            Parent = parent;
            Node = node;
            Node.Name = name;
        }
    }
}
