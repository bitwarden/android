namespace CommunityToolkit.Maui.Behaviors;

/// <summary>
/// Provides data for the <see cref="TouchBehavior.InteractionStatusChanged"/> event.
/// </summary>
public class TouchInteractionStatusChangedEventArgs : EventArgs
{
	internal TouchInteractionStatusChangedEventArgs(TouchInteractionStatus touchInteractionStatus)
		=> TouchInteractionStatus = touchInteractionStatus;

	/// <summary>
	/// Gets the current touch interaction status.
	/// </summary>
	public TouchInteractionStatus TouchInteractionStatus { get; }
}