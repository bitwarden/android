namespace CommunityToolkit.Maui.Behaviors;

/// <summary>
/// Provides data for the <see cref="TouchBehavior.HoverStateChanged"/> event.
/// </summary>
public class HoverStateChangedEventArgs : EventArgs
{
	internal HoverStateChangedEventArgs(HoverState state)
		=> State = state;

	/// <summary>
	/// Gets the new <see cref="HoverState"/> of the element.
	/// </summary>
	public HoverState State { get; }
}