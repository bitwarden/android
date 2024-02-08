﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using CommunityToolkit.Maui.Extensions;
using static System.Math;

namespace CommunityToolkit.Maui.Behaviors;

sealed class GestureManager : IDisposable
{
	const int animationProgressDelay = 10;

	Color? defaultBackgroundColor;

	CancellationTokenSource? longPressTokenSource;

	CancellationTokenSource? animationTokenSource;

	Func<TouchBehavior, TouchState, HoverState, int, Easing?, CancellationToken, Task>? animationTaskFactory;

	double? durationMultiplier;

	double animationProgress;

	TouchState animationState;

	internal void HandleTouch(TouchBehavior sender, TouchStatus status)
	{
		if (sender.IsDisabled)
		{
			return;
		}

		var canExecuteAction = sender.CanExecute;
		if (status != TouchStatus.Started || canExecuteAction)
		{
			if (!canExecuteAction)
			{
				status = TouchStatus.Canceled;
			}

			var state = status == TouchStatus.Started
				? TouchState.Pressed
				: TouchState.Normal;

			if (status == TouchStatus.Started)
			{
				animationProgress = 0;
				animationState = state;
			}

			var isToggled = sender.IsToggled;
			if (isToggled.HasValue)
			{
				if (status != TouchStatus.Started)
				{
					durationMultiplier = (animationState == TouchState.Pressed && !isToggled.Value) ||
						(animationState == TouchState.Normal && isToggled.Value)
						? 1 - animationProgress
						: animationProgress;

					UpdateStatusAndState(sender, status, state);

					if (status == TouchStatus.Canceled)
					{
						sender.ForceUpdateState(false);
						return;
					}

					OnTapped(sender);
					sender.IsToggled = !isToggled;
					return;
				}

				state = isToggled.Value
					? TouchState.Normal
					: TouchState.Pressed;
			}

			UpdateStatusAndState(sender, status, state);
		}

		if (status == TouchStatus.Completed)
		{
			OnTapped(sender);
		}
	}

	internal void HandleUserInteraction(TouchBehavior sender, TouchInteractionStatus interactionStatus)
	{
		if (sender.InteractionStatus != interactionStatus)
		{
			sender.InteractionStatus = interactionStatus;
			sender.RaiseInteractionStatusChanged();
		}
	}

	internal void HandleHover(TouchBehavior sender, HoverStatus status)
	{
		if (!sender.Element?.IsEnabled ?? true)
		{
			return;
		}

		var hoverState = status == HoverStatus.Entered
			? HoverState.Hovered
			: HoverState.Normal;

		if (sender.HoverState != hoverState)
		{
			sender.HoverState = hoverState;
			sender.RaiseHoverStateChanged();
		}

		if (sender.HoverStatus != status)
		{
			sender.HoverStatus = status;
			sender.RaiseHoverStatusChanged();
		}
	}

	internal async Task ChangeStateAsync(TouchBehavior sender, bool animated)
	{
		var status = sender.Status;
		var state = sender.State;
		var hoverState = sender.HoverState;

		AbortAnimations(sender);
		animationTokenSource = new CancellationTokenSource();
		var token = animationTokenSource.Token;

		var isToggled = sender.IsToggled;

		if (sender.Element is not null)
		{
			UpdateVisualState(sender.Element, state, hoverState);
		}

		if (!animated)
		{
			if (isToggled.HasValue)
			{
				state = isToggled.Value
					? TouchState.Pressed
					: TouchState.Normal;
			}

			var durationMultiplier = this.durationMultiplier;
			this.durationMultiplier = null;

			await RunAnimationTask(sender, state, hoverState, animationTokenSource.Token, durationMultiplier.GetValueOrDefault()).ConfigureAwait(false);
			return;
		}

		var pulseCount = sender.PulseCount;

		if (pulseCount == 0 || (state == TouchState.Normal && !isToggled.HasValue))
		{
			if (isToggled.HasValue)
			{
				Console.WriteLine($"Touch state: {status}");
				var r = (status == TouchStatus.Started && isToggled.Value) ||
					(status != TouchStatus.Started && !isToggled.Value);
				state = r
					? TouchState.Normal
					: TouchState.Pressed;
			}

			await RunAnimationTask(sender, state, hoverState, animationTokenSource.Token).ConfigureAwait(false);
			return;
		}
		do
		{
			var rippleState = isToggled.HasValue && isToggled.Value
				? TouchState.Normal
				: TouchState.Pressed;

			await RunAnimationTask(sender, rippleState, hoverState, animationTokenSource.Token);
			if (token.IsCancellationRequested)
			{
				return;
			}

			rippleState = isToggled.HasValue && isToggled.Value
				? TouchState.Pressed
				: TouchState.Normal;

			await RunAnimationTask(sender, rippleState, hoverState, animationTokenSource.Token);
			if (token.IsCancellationRequested)
			{
				return;
			}
		}
		while (--pulseCount != 0);
	}

	internal void HandleLongPress(TouchBehavior sender)
	{
		if (sender.State == TouchState.Normal)
		{
			longPressTokenSource?.Cancel();
			longPressTokenSource?.Dispose();
			longPressTokenSource = null;
			return;
		}

		if (sender.LongPressCommand is null || sender.InteractionStatus == TouchInteractionStatus.Completed)
		{
			return;
		}

		longPressTokenSource = new CancellationTokenSource();
		Task.Delay(sender.LongPressDuration, longPressTokenSource.Token).ContinueWith(t =>
		{
			if (t.IsFaulted && t.Exception != null)
			{
				throw t.Exception;
			}

			if (t.IsCanceled)
			{
				return;
			}

			var longPressAction = new Action(() =>
			{
				sender.HandleUserInteraction(TouchInteractionStatus.Completed);
				sender.RaiseLongPressCompleted();
			});
			
			if (sender.Dispatcher.IsDispatchRequired)
			{
				sender.Dispatcher.Dispatch(longPressAction);
			}
			else
			{
				longPressAction.Invoke();
			}
		});
	}

	internal void SetCustomAnimationTask(Func<TouchBehavior, TouchState, HoverState, int, Easing?, CancellationToken, Task>? animationTaskFactory)
		=> this.animationTaskFactory = animationTaskFactory;

	internal void Reset()
	{
		SetCustomAnimationTask(null);
		defaultBackgroundColor = default;
	}

	internal void OnTapped(TouchBehavior sender)
	{
		if (!sender.CanExecute || (sender.LongPressCommand != null && sender.InteractionStatus == TouchInteractionStatus.Completed))
		{
			return;
		}

		if (DeviceInfo.Platform == DevicePlatform.Android)
		{
			HandleCollectionViewSelection(sender);
		}

		if (sender.Element is IButtonController button)
		{
			button.SendClicked();
		}

		sender.RaiseCompleted();
	}

	void HandleCollectionViewSelection(TouchBehavior sender)
	{
		CollectionView? parent = null;
		VisualElement? result = null;
		if (!sender.Element?.TryFindParentElementWithParentOfType(out result, out parent) ?? true)
		{
			return;
		}

		var collectionView = parent ?? throw new NullReferenceException();

		var item = result?.BindingContext ?? result ?? throw new NullReferenceException();

		switch (collectionView.SelectionMode)
		{
			case SelectionMode.Single:
				collectionView.SelectedItem = item;
				break;
			case SelectionMode.Multiple:
				var selectedItems = collectionView.SelectedItems?.ToList() ?? new List<object>();

				if (selectedItems.Contains(item))
				{
					selectedItems.Remove(item);
				}
				else
				{
					selectedItems.Add(item);
				}

				collectionView.UpdateSelectedItems(selectedItems);
				break;
		}
	}

	internal void AbortAnimations(TouchBehavior sender)
	{
		animationTokenSource?.Cancel();
		animationTokenSource?.Dispose();
		animationTokenSource = null;
		var element = sender.Element;
		if (element == null)
		{
			return;
		}

		element.AbortAnimations();
	}

	void UpdateStatusAndState(TouchBehavior sender, TouchStatus status, TouchState state)
	{
		sender.Status = status;
		sender.RaiseStatusChanged();

		if (sender.State != state || status != TouchStatus.Canceled)
		{
			sender.State = state;
			sender.RaiseStateChanged();
		}
	}

	void UpdateVisualState(VisualElement visualElement, TouchState touchState, HoverState hoverState)
	{
		var state = touchState == TouchState.Pressed
			? TouchBehavior.PressedVisualState
			: hoverState == HoverState.Hovered
				? TouchBehavior.HoveredVisualState
				: TouchBehavior.UnpressedVisualState;

		VisualStateManager.GoToState(visualElement, state);
	}

	async Task SetBackgroundImageAsync(TouchBehavior sender, TouchState touchState, HoverState hoverState, int duration, CancellationToken token)
	{
		var normalBackgroundImageSource = sender.NormalBackgroundImageSource;
		var pressedBackgroundImageSource = sender.PressedBackgroundImageSource;
		var hoveredBackgroundImageSource = sender.HoveredBackgroundImageSource;

		if (normalBackgroundImageSource is null &&
			pressedBackgroundImageSource is null &&
			hoveredBackgroundImageSource is null)
		{
			return;
		}

		var aspect = sender.BackgroundImageAspect;
		var source = normalBackgroundImageSource;
		if (touchState == TouchState.Pressed)
		{
			if (sender.IsSet(TouchBehavior.PressedBackgroundImageAspectProperty))
			{
				aspect = sender.PressedBackgroundImageAspect;
			}

			source = pressedBackgroundImageSource;
		}
		else if (hoverState == HoverState.Hovered)
		{
			if (sender.IsSet(TouchBehavior.HoveredBackgroundImageAspectProperty))
			{
				aspect = sender.HoveredBackgroundImageAspect;
			}

			if (sender.IsSet(TouchBehavior.HoveredBackgroundImageSourceProperty))
			{
				source = hoveredBackgroundImageSource;
			}
		}
		else
		{
			if (sender.IsSet(TouchBehavior.NormalBackgroundImageAspectProperty))
			{
				aspect = sender.NormalBackgroundImageAspect;
			}
		}

		try
		{
			if (sender.ShouldSetImageOnAnimationEnd && duration > 0)
			{
				await Task.Delay(duration, token);
			}
		}
		catch (TaskCanceledException)
		{
			return;
		}

		if (sender.Element is Image image)
		{
			using (image.Batch())
			{
				image.Aspect = aspect;
				image.Source = source;
			}
		}
	}

	Task<bool> SetBackgroundColor(TouchBehavior sender, TouchState touchState, HoverState hoverState, int duration, Easing? easing)
	{
		var normalBackgroundColor = sender.NormalBackgroundColor;
		var pressedBackgroundColor = sender.PressedBackgroundColor;
		var hoveredBackgroundColor = sender.HoveredBackgroundColor;

		if (sender.Element == null
			|| (normalBackgroundColor is null
				&& pressedBackgroundColor is null
				&& hoveredBackgroundColor is null))
		{
			return Task.FromResult(false);
		}

		var element = sender.Element;
		if (defaultBackgroundColor == default)
		{
			defaultBackgroundColor = element.BackgroundColor;
		}

		var color = GetBackgroundColor(normalBackgroundColor);

		if (touchState == TouchState.Pressed)
		{
			color = GetBackgroundColor(pressedBackgroundColor);
		}
		else if (hoverState == HoverState.Hovered && sender.IsSet(TouchBehavior.HoveredBackgroundColorProperty))
		{
			color = GetBackgroundColor(hoveredBackgroundColor);
		}

		if (duration <= 0)
		{
			element.AbortAnimations();
			element.BackgroundColor = color;
			return Task.FromResult(true);
		}

		return element.ColorTo(color ?? Colors.Transparent, (uint)duration, easing);
	}

	Task<bool> SetOpacity(TouchBehavior sender, TouchState touchState, HoverState hoverState, int duration, Easing? easing)
	{
		var normalOpacity = sender.NormalOpacity;
		var pressedOpacity = sender.PressedOpacity;
		var hoveredOpacity = sender.HoveredOpacity;

		if (Abs(normalOpacity - 1) <= double.Epsilon &&
			Abs(pressedOpacity - 1) <= double.Epsilon &&
			Abs(hoveredOpacity - 1) <= double.Epsilon)
		{
			return Task.FromResult(false);
		}

		var opacity = normalOpacity;

		if (touchState == TouchState.Pressed)
		{
			opacity = pressedOpacity;
		}
		else if (hoverState == HoverState.Hovered && sender.IsSet(TouchBehavior.HoveredOpacityProperty))
		{
			opacity = hoveredOpacity;
		}

		var element = sender.Element;
		if (duration <= 0 && element is not null)
		{
			element.AbortAnimations();
			element.Opacity = opacity;
			return Task.FromResult(true);
		}

		return element is null ? 
			Task.FromResult(false) : 
			element.FadeTo(opacity, (uint)Abs(duration), easing);
	}

	Task SetScale(TouchBehavior sender, TouchState touchState, HoverState hoverState, int duration, Easing? easing)
	{
		var normalScale = sender.NormalScale;
		var pressedScale = sender.PressedScale;
		var hoveredScale = sender.HoveredScale;

		if (Abs(normalScale - 1) <= double.Epsilon &&
			Abs(pressedScale - 1) <= double.Epsilon &&
			Abs(hoveredScale - 1) <= double.Epsilon)
		{
			return Task.FromResult(false);
		}

		var scale = normalScale;

		if (touchState == TouchState.Pressed)
		{
			scale = pressedScale;
		}
		else if (hoverState == HoverState.Hovered && sender.IsSet(TouchBehavior.HoveredScaleProperty))
		{
			scale = hoveredScale;
		}

		var element = sender.Element;
		if (element is null)
		{
			return Task.FromResult(false);
		}

		if (duration <= 0)
		{
			element.AbortAnimations(nameof(SetScale));
			element.Scale = scale;
			return Task.FromResult(true);
		}

		var animationCompletionSource = new TaskCompletionSource<bool>();
		element.Animate(nameof(SetScale), v =>
		{
			if (double.IsNaN(v))
			{
				return;
			}

			element.Scale = v;
		}, element.Scale, scale, 16, (uint)Abs(duration), easing, (v, b) => animationCompletionSource.SetResult(b));
		return animationCompletionSource.Task;
	}

	Task SetTranslation(TouchBehavior sender, TouchState touchState, HoverState hoverState, int duration, Easing? easing)
	{
		var normalTranslationX = sender.NormalTranslationX;
		var pressedTranslationX = sender.PressedTranslationX;
		var hoveredTranslationX = sender.HoveredTranslationX;

		var normalTranslationY = sender.NormalTranslationY;
		var pressedTranslationY = sender.PressedTranslationY;
		var hoveredTranslationY = sender.HoveredTranslationY;

		if (Abs(normalTranslationX) <= double.Epsilon
			&& Abs(pressedTranslationX) <= double.Epsilon
			&& Abs(hoveredTranslationX) <= double.Epsilon
			&& Abs(normalTranslationY) <= double.Epsilon
			&& Abs(pressedTranslationY) <= double.Epsilon
			&& Abs(hoveredTranslationY) <= double.Epsilon)
		{
			return Task.FromResult(false);
		}

		var translationX = normalTranslationX;
		var translationY = normalTranslationY;

		if (touchState == TouchState.Pressed)
		{
			translationX = pressedTranslationX;
			translationY = pressedTranslationY;
		}
		else if (hoverState == HoverState.Hovered)
		{
			if (sender.IsSet(TouchBehavior.HoveredTranslationXProperty))
			{
				translationX = hoveredTranslationX;
			}

			if (sender.IsSet(TouchBehavior.HoveredTranslationYProperty))
			{
				translationY = hoveredTranslationY;
			}
		}

		var element = sender.Element;
		if (duration <= 0 && element != null)
		{
			element.AbortAnimations();
			element.TranslationX = translationX;
			element.TranslationY = translationY;
			return Task.FromResult(true);
		}

		return element?.TranslateTo(translationX, translationY, (uint)Abs(duration), easing) ?? Task.FromResult(false);
	}

	Task SetRotation(TouchBehavior sender, TouchState touchState, HoverState hoverState, int duration, Easing? easing)
	{
		var normalRotation = sender.NormalRotation;
		var pressedRotation = sender.PressedRotation;
		var hoveredRotation = sender.HoveredRotation;

		if (Abs(normalRotation) <= double.Epsilon
			&& Abs(pressedRotation) <= double.Epsilon
			&& Abs(hoveredRotation) <= double.Epsilon)
		{
			return Task.FromResult(false);
		}

		var rotation = normalRotation;

		if (touchState == TouchState.Pressed)
		{
			rotation = pressedRotation;
		}
		else if (hoverState == HoverState.Hovered && sender.IsSet(TouchBehavior.HoveredRotationProperty))
		{
			rotation = hoveredRotation;
		}

		var element = sender.Element;
		if (duration <= 0 && element != null)
		{
			element.AbortAnimations();
			element.Rotation = rotation;
			return Task.FromResult(true);
		}

		return element?.RotateTo(rotation, (uint)Abs(duration), easing) ?? Task.FromResult(false);
	}

	Task SetRotationX(TouchBehavior sender, TouchState touchState, HoverState hoverState, int duration, Easing? easing)
	{
		var normalRotationX = sender.NormalRotationX;
		var pressedRotationX = sender.PressedRotationX;
		var hoveredRotationX = sender.HoveredRotationX;

		if (Abs(normalRotationX) <= double.Epsilon &&
			Abs(pressedRotationX) <= double.Epsilon &&
			Abs(hoveredRotationX) <= double.Epsilon)
		{
			return Task.FromResult(false);
		}

		var rotationX = normalRotationX;

		if (touchState == TouchState.Pressed)
		{
			rotationX = pressedRotationX;
		}
		else if (hoverState == HoverState.Hovered && sender.IsSet(TouchBehavior.HoveredRotationXProperty))
		{
			rotationX = hoveredRotationX;
		}

		var element = sender.Element;
		if (duration <= 0 && element != null)
		{
			element.AbortAnimations();
			element.RotationX = rotationX;
			return Task.FromResult(true);
		}

		return element?.RotateXTo(rotationX, (uint)Abs(duration), easing) ?? Task.FromResult(false);
	}

	Task SetRotationY(TouchBehavior sender, TouchState touchState, HoverState hoverState, int duration, Easing? easing)
	{
		var normalRotationY = sender.NormalRotationY;
		var pressedRotationY = sender.PressedRotationY;
		var hoveredRotationY = sender.HoveredRotationY;

		if (Abs(normalRotationY) <= double.Epsilon &&
			Abs(pressedRotationY) <= double.Epsilon &&
			Abs(hoveredRotationY) <= double.Epsilon)
		{
			return Task.FromResult(false);
		}

		var rotationY = normalRotationY;

		if (touchState == TouchState.Pressed)
		{
			rotationY = pressedRotationY;
		}
		else if (hoverState == HoverState.Hovered && sender.IsSet(TouchBehavior.HoveredRotationYProperty))
		{
			rotationY = hoveredRotationY;
		}

		var element = sender.Element;
		if (duration <= 0 && element != null)
		{
			element.AbortAnimations();
			element.RotationY = rotationY;
			return Task.FromResult(true);
		}

		return element?.RotateYTo(rotationY, (uint)Abs(duration), easing) ?? Task.FromResult(false);
	}

	Color? GetBackgroundColor(Color? color)
		=> color is not null
			? color
			: defaultBackgroundColor;

	Task RunAnimationTask(TouchBehavior sender, TouchState touchState, HoverState hoverState, CancellationToken token, double? durationMultiplier = null)
	{
		if (sender.Element == null)
		{
			return Task.FromResult(false);
		}

		var duration = sender.AnimationDuration;
		var easing = sender.AnimationEasing;

		if (touchState == TouchState.Pressed)
		{
			if (sender.IsSet(TouchBehavior.PressedAnimationDurationProperty))
			{
				duration = sender.PressedAnimationDuration;
			}

			if (sender.IsSet(TouchBehavior.PressedAnimationEasingProperty))
			{
				easing = sender.PressedAnimationEasing;
			}
		}
		else if (hoverState == HoverState.Hovered)
		{
			if (sender.IsSet(TouchBehavior.HoveredAnimationDurationProperty))
			{
				duration = sender.HoveredAnimationDuration;
			}

			if (sender.IsSet(TouchBehavior.HoveredAnimationEasingProperty))
			{
				easing = sender.HoveredAnimationEasing;
			}
		}
		else
		{
			if (sender.IsSet(TouchBehavior.NormalAnimationDurationProperty))
			{
				duration = sender.NormalAnimationDuration;
			}

			if (sender.IsSet(TouchBehavior.NormalAnimationEasingProperty))
			{
				easing = sender.NormalAnimationEasing;
			}
		}

		if (durationMultiplier.HasValue)
		{
			duration = (int)durationMultiplier.Value * duration;
		}

		duration = Max(duration, 0);

		return Task.WhenAll(
			animationTaskFactory?.Invoke(sender, touchState, hoverState, duration, easing, token) ?? Task.FromResult(true),
			SetBackgroundImageAsync(sender, touchState, hoverState, duration, token),
			SetBackgroundColor(sender, touchState, hoverState, duration, easing),
			SetOpacity(sender, touchState, hoverState, duration, easing),
			SetScale(sender, touchState, hoverState, duration, easing),
			SetTranslation(sender, touchState, hoverState, duration, easing),
			SetRotation(sender, touchState, hoverState, duration, easing),
			SetRotationX(sender, touchState, hoverState, duration, easing),
			SetRotationY(sender, touchState, hoverState, duration, easing),
			Task.Run(async () =>
			{
				animationProgress = 0;
				animationState = touchState;

				for (var progress = animationProgressDelay; progress < duration; progress += animationProgressDelay)
				{
					await Task.Delay(animationProgressDelay).ConfigureAwait(false);
					if (token.IsCancellationRequested)
					{
						return;
					}

					animationProgress = (double)progress / duration;
				}
				animationProgress = 1;
			}));
	}

	public void Dispose()
	{
		throw new NotImplementedException();
	}
}
