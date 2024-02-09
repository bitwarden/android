namespace CommunityToolkit.Maui.Behaviors;

/// <summary>
/// Provides data for the <see cref="TouchBehavior.StatusChanged"/> event.
/// </summary>
public enum TouchState
{
	/// <summary>
	/// The pointer is not over the element.
	/// </summary>
	Normal,
	
	/// <summary>
	/// The pointer is over the element.
	/// </summary>
	Pressed
}