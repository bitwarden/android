namespace CommunityToolkit.Maui.Behaviors;

/// <summary>
/// Provides data for the <see cref="TouchBehavior.Completed"/> event.
/// </summary>
public enum TouchInteractionStatus
{
	/// <summary>
	/// The touch interaction has started.
	/// </summary>
	Started,
	
	/// <summary>
	/// The touch interaction has completed.
	/// </summary>
	Completed
}