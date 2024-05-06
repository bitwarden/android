namespace CommunityToolkit.Maui.Behaviors;

/// <summary>
/// Provides data for the <see cref="TouchBehavior.LongPressCompleted"/> event.
/// </summary>
public class LongPressCompletedEventArgs : EventArgs
{
	internal LongPressCompletedEventArgs(object? parameter)
		=> Parameter = parameter;

	/// <summary>
	/// Gets the parameter of the <see cref="TouchBehavior.LongPressCompleted"/> event.
	/// </summary>
	public object? Parameter { get; }
}