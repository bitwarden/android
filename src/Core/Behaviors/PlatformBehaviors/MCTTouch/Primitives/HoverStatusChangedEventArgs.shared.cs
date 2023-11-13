namespace CommunityToolkit.Maui.Behaviors;

/// <summary>
/// Provides data for the <see cref="TouchBehavior.HoverStatusChanged"/> event.
/// </summary>
public class HoverStatusChangedEventArgs : EventArgs
{
	internal HoverStatusChangedEventArgs(HoverStatus status)
		=> Status = status;

	/// <summary>
	/// Gets the new <see cref="HoverStatus"/> of the element.
	/// </summary>
	public HoverStatus Status { get; }
}