import { useEffect, useRef } from 'react';
import { attachFormValiditySync } from '../../utils/formValidityFallback';

export function useFormValidity<T extends HTMLFormElement>(dependency?: unknown) {
  const formRef = useRef<T | null>(null);

  useEffect(() => {
    return attachFormValiditySync(formRef.current);
  }, [dependency]);

  return formRef;
}
