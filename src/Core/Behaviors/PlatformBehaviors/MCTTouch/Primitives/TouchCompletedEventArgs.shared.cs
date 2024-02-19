namespace CommunityToolkit.Maui.Behaviors;

/// <summary>
/// Provides data for the <see cref="TouchBehavior.Completed"/> event.
/// </summary>
public class TouchCompletedEventArgs : EventArgs

{
	/// <summary>
	/// Initializes a new instance of the <see cref="TouchCompletedEventArgs"/> class.
	/// </summary>
	internal TouchCompletedEventArgs(object? parameter)
		=> Parameter = parameter;

	/// <summary>
	/// Gets the parameter associated with the touch event.
	/// </summary>
	public object? Parameter { get; }
}