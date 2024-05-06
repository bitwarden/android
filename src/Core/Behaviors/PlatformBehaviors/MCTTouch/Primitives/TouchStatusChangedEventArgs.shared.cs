namespace CommunityToolkit.Maui.Behaviors;

/// <summary>
/// Provides data for the <see cref="TouchBehavior.StatusChanged"/> event.
/// </summary>
public class TouchStatusChangedEventArgs : EventArgs
{
	internal TouchStatusChangedEventArgs(TouchStatus status)
		=> Status = status;

	/// <summary>
	/// Gets the current touch status.
	/// </summary>
	public TouchStatus Status { get; }
}