type InteractionState = {
  hasInteracted: boolean;
  hasBlurred: boolean;
};

export function attachFormValiditySync(formElement: HTMLFormElement | null): () => void {
  if (!formElement) {
    return () => {};
  }

  const dirtyState = new WeakMap<Element, InteractionState>();
  const supportsNativeUserInvalid =
    typeof CSS !== 'undefined' && typeof CSS.supports === 'function'
      ? CSS.supports('selector(:user-invalid)')
      : false;

  const syncAriaAndFallback = (target: EventTarget | null) => {
    if (
      !(target instanceof HTMLInputElement) &&
      !(target instanceof HTMLSelectElement) &&
      !(target instanceof HTMLTextAreaElement)
    ) {
      return;
    }

    if (supportsNativeUserInvalid) {
      const isUserInvalid = target.matches(':user-invalid');
      target.setAttribute('aria-invalid', isUserInvalid ? 'true' : 'false');
      return;
    }

    const isValid = target.checkValidity();
    target.classList.toggle('user-invalid-fallback', !isValid);
    target.classList.toggle('user-valid-fallback', isValid);
    if (!isValid) {
      target.setAttribute('aria-invalid', 'true');
    } else {
      target.removeAttribute('aria-invalid');
    }

    const container = target.closest('.field-group');
    if (container) {
      const hasError = container.querySelector('.user-invalid-fallback');
      container.classList.toggle('has-error-fallback', Boolean(hasError));
    }
  };

  const handleBlur = (event: Event) => {
    const target = event.target;
    if (
      !(target instanceof HTMLInputElement) &&
      !(target instanceof HTMLSelectElement) &&
      !(target instanceof HTMLTextAreaElement)
    ) {
      return;
    }
    const state = dirtyState.get(target) ?? { hasInteracted: false, hasBlurred: false };
    state.hasBlurred = true;
    dirtyState.set(target, state);

    if (supportsNativeUserInvalid || state.hasInteracted) {
      syncAriaAndFallback(target);
    }
  };

  const handleInputOrChange = (event: Event) => {
    const target = event.target;
    if (
      !(target instanceof HTMLInputElement) &&
      !(target instanceof HTMLSelectElement) &&
      !(target instanceof HTMLTextAreaElement)
    ) {
      return;
    }
    const state = dirtyState.get(target) ?? { hasInteracted: false, hasBlurred: false };
    state.hasInteracted = true;
    dirtyState.set(target, state);

    if (supportsNativeUserInvalid) {
      if (target.hasAttribute('aria-invalid')) {
        syncAriaAndFallback(target);
      }
    } else if (state.hasBlurred) {
      syncAriaAndFallback(target);
    }
  };

  const handleReset = () => {
    const controls = Array.from(formElement.elements);
    for (const control of controls) {
      dirtyState.delete(control);
      control.classList.remove('user-invalid-fallback', 'user-valid-fallback');
      control.removeAttribute('aria-invalid');
    }
    formElement.querySelectorAll('.has-error-fallback').forEach((el) => {
      el.classList.remove('has-error-fallback');
    });
  };

  formElement.addEventListener('blur', handleBlur, true);
  formElement.addEventListener('input', handleInputOrChange);
  formElement.addEventListener('change', handleInputOrChange);
  formElement.addEventListener('reset', handleReset, true);

  return () => {
    formElement.removeEventListener('blur', handleBlur, true);
    formElement.removeEventListener('input', handleInputOrChange);
    formElement.removeEventListener('change', handleInputOrChange);
    formElement.removeEventListener('reset', handleReset, true);
  };
}
