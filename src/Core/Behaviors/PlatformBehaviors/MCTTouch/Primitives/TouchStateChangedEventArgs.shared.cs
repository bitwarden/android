namespace CommunityToolkit.Maui.Behaviors;

/// <summary>
/// Provides data for the <see cref="TouchBehavior.StateChanged"/> event.
/// </summary>
public class TouchStateChangedEventArgs : EventArgs
{
	internal TouchStateChangedEventArgs(TouchState state)
		=> State = state;

	/// <summary>
	/// Gets the current state of the touch event.
	/// </summary>
	public TouchState State { get; }
}