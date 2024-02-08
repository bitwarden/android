#if IOS
using AsyncAwaitBestPractices;
using CoreGraphics;
using Foundation;
using Microsoft.Maui.Controls.Compatibility.Platform.iOS;
using Microsoft.Maui.Platform;
using UIKit;

namespace CommunityToolkit.Maui.Behaviors;

public partial class TouchBehavior
{
	private UIGestureRecognizer? touchGesture;
	private UIGestureRecognizer? hoverGesture;

	/// <summary>
	/// Attaches the behavior to the platform view.
	/// </summary>
	/// <param name="bindable">Maui Visual Element</param>
	/// <param name="platformView">Native View</param>
	protected override void OnAttachedTo(VisualElement bindable, UIView platformView)
	{
		Element = bindable;

		touchGesture = new TouchUITapGestureRecognizer(this);

		if (((platformView as IVisualNativeElementRenderer)?.Control ?? platformView) is UIButton button)
		{
			button.AllTouchEvents += PreventButtonHighlight;
			((TouchUITapGestureRecognizer) touchGesture).IsButton = true;
		}

		platformView.AddGestureRecognizer(touchGesture);

		if (UIDevice.CurrentDevice.CheckSystemVersion(13, 0))
		{
			hoverGesture = new UIHoverGestureRecognizer(OnHover);
			platformView.AddGestureRecognizer(hoverGesture);
		}

		platformView.UserInteractionEnabled = true;
	}

	/// <summary>
	/// Detaches the behavior from the platform view.
	/// </summary>
	/// <param name="bindable">Maui Visual Element</param>
	/// <param name="platformView">Native View</param>
	protected override void OnDetachedFrom(VisualElement bindable, UIView platformView)
	{
		if (((platformView as IVisualNativeElementRenderer)?.Control ?? platformView) is UIButton button)
		{
			button.AllTouchEvents -= PreventButtonHighlight;
		}

		if (touchGesture != null)
		{
			platformView?.RemoveGestureRecognizer(touchGesture);
			touchGesture?.Dispose();
			touchGesture = null;
		}

		if (hoverGesture != null)
		{
			platformView?.RemoveGestureRecognizer(hoverGesture);
			hoverGesture?.Dispose();
			hoverGesture = null;
		}

		Element = null;
	}

	private void OnHover()
	{
		if (IsDisabled)
		{
			return;
		}

		switch (hoverGesture?.State)
		{
			case UIGestureRecognizerState.Began:
			case UIGestureRecognizerState.Changed:
				HandleHover(HoverStatus.Entered);
				break;
			case UIGestureRecognizerState.Ended:
				HandleHover(HoverStatus.Exited);
				break;
		}
	}

	private void PreventButtonHighlight(object? sender, EventArgs args)
	{
		if (sender is not UIButton button)
		{
			throw new ArgumentException($"{nameof(sender)} must be Type {nameof(UIButton)}", nameof(sender));
		}

		button.Highlighted = false;
	}
}

internal sealed class TouchUITapGestureRecognizer : UIGestureRecognizer
{
	private TouchBehavior behavior;
	private float? defaultRadius;
	private float? defaultShadowRadius;
	private float? defaultShadowOpacity;
	private CGPoint? startPoint;

	public TouchUITapGestureRecognizer(TouchBehavior behavior)
	{
		this.behavior = behavior;
		CancelsTouchesInView = false;
		Delegate = new TouchUITapGestureRecognizerDelegate();
	}

	public bool IsCanceled { get; set; } = true;

	public bool IsButton { get; set; }

	public override void TouchesBegan(NSSet touches, UIEvent evt)
	{
		if (behavior?.IsDisabled ?? true)
		{
			return;
		}

		IsCanceled = false;
		startPoint = GetTouchPoint(touches);

		HandleTouch(TouchStatus.Started, TouchInteractionStatus.Started).SafeFireAndForget();

		base.TouchesBegan(touches, evt);
	}

	public override void TouchesEnded(NSSet touches, UIEvent evt)
	{
		if (behavior?.IsDisabled ?? true)
		{
			return;
		}

		HandleTouch(behavior?.Status == TouchStatus.Started ? TouchStatus.Completed : TouchStatus.Canceled, TouchInteractionStatus.Completed).SafeFireAndForget();

		IsCanceled = true;

		base.TouchesEnded(touches, evt);
	}

	public override void TouchesCancelled(NSSet touches, UIEvent evt)
	{
		if (behavior?.IsDisabled ?? true)
		{
			return;
		}

		HandleTouch(TouchStatus.Canceled, TouchInteractionStatus.Completed).SafeFireAndForget();

		IsCanceled = true;

		base.TouchesCancelled(touches, evt);
	}

	public override void TouchesMoved(NSSet touches, UIEvent evt)
	{
		if (behavior?.IsDisabled ?? true)
		{
			return;
		}

		var disallowTouchThreshold = behavior.DisallowTouchThreshold;
		var point = GetTouchPoint(touches);
		if (point != null && startPoint != null && disallowTouchThreshold > 0)
		{
			var diffX = Math.Abs(point.Value.X - startPoint.Value.X);
			var diffY = Math.Abs(point.Value.Y - startPoint.Value.Y);
			var maxDiff = Math.Max(diffX, diffY);
			if (maxDiff > disallowTouchThreshold)
			{
				HandleTouch(TouchStatus.Canceled, TouchInteractionStatus.Completed).SafeFireAndForget();
				IsCanceled = true;
				base.TouchesMoved(touches, evt);
				return;
			}
		}

		var status = point != null && View?.Bounds.Contains(point.Value) is true
			? TouchStatus.Started
			: TouchStatus.Canceled;

		if (behavior?.Status != status)
		{
			HandleTouch(status).SafeFireAndForget();
		}

		if (status == TouchStatus.Canceled)
		{
			IsCanceled = true;
		}

		base.TouchesMoved(touches, evt);
	}

	public async Task HandleTouch(TouchStatus status, TouchInteractionStatus? interactionStatus = null)
	{
		if (IsCanceled || behavior == null)
		{
			return;
		}

		if (behavior?.IsDisabled ?? true)
		{
			return;
		}

		var canExecuteAction = behavior.CanExecute;

		if (interactionStatus == TouchInteractionStatus.Started)
		{
			behavior?.HandleUserInteraction(TouchInteractionStatus.Started);
			interactionStatus = null;
		}

		behavior?.HandleTouch(status);
		if (interactionStatus.HasValue)
		{
			behavior?.HandleUserInteraction(interactionStatus.Value);
		}

		if (behavior == null || behavior.Element is null || (!behavior.NativeAnimation && !IsButton) || (!canExecuteAction && status == TouchStatus.Started))
		{
			return;
		}

		var color = behavior.NativeAnimationColor;
		var radius = behavior.NativeAnimationRadius;
		var shadowRadius = behavior.NativeAnimationShadowRadius;
		var isStarted = status == TouchStatus.Started;
		defaultRadius = (float?) (defaultRadius ?? View.Layer.CornerRadius);
		defaultShadowRadius = (float?) (defaultShadowRadius ?? View.Layer.ShadowRadius);
		defaultShadowOpacity ??= View.Layer.ShadowOpacity;

		var tcs = new TaskCompletionSource<UIViewAnimatingPosition>();
		UIViewPropertyAnimator.CreateRunningPropertyAnimator(.2, 0, UIViewAnimationOptions.AllowUserInteraction,
			() =>
			{
				if (color == default(Color))
				{
					View.Layer.Opacity = isStarted ? 0.5f : (float) behavior.Element.Opacity;
				}
				else
				{
					View.Layer.BackgroundColor = (isStarted ? color : behavior.Element.BackgroundColor).ToCGColor();
				}

				View.Layer.CornerRadius = isStarted ? radius : defaultRadius.GetValueOrDefault();

				if (shadowRadius >= 0)
				{
					View.Layer.ShadowRadius = isStarted ? shadowRadius : defaultShadowRadius.GetValueOrDefault();
					View.Layer.ShadowOpacity = isStarted ? 0.7f : defaultShadowOpacity.GetValueOrDefault();
				}
			}, endPos => tcs.SetResult(endPos));
		await tcs.Task;
	}

	protected override void Dispose(bool disposing)
	{
		if (disposing)
		{
			Delegate.Dispose();
		}

		base.Dispose(disposing);
	}

	private CGPoint? GetTouchPoint(NSSet touches)
	{
		return (touches?.AnyObject as UITouch)?.LocationInView(View);
	}

	private class TouchUITapGestureRecognizerDelegate : UIGestureRecognizerDelegate
	{
		public override bool ShouldRecognizeSimultaneously(UIGestureRecognizer gestureRecognizer, UIGestureRecognizer otherGestureRecognizer)
		{
			if (gestureRecognizer is TouchUITapGestureRecognizer touchGesture && otherGestureRecognizer is UIPanGestureRecognizer &&
			    otherGestureRecognizer.State == UIGestureRecognizerState.Began)
			{
				touchGesture.HandleTouch(TouchStatus.Canceled, TouchInteractionStatus.Completed).SafeFireAndForget();
				touchGesture.IsCanceled = true;
			}

			return true;
		}

		public override bool ShouldReceiveTouch(UIGestureRecognizer recognizer, UITouch touch)
		{
			if (recognizer.View.IsDescendantOfView(touch.View))
			{
				return true;
			}

			return recognizer.View.Subviews.Any(view => view == touch.View);
		}
	}
}
#endif
