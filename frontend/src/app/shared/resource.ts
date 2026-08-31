import { WritableResource } from '@angular/core';

/**
 * Reads a resource's value without throwing.
 *
 * `resource.value()` throws a `ResourceValueError` whenever the resource is
 * in an error state (e.g. the backend answered with a 404 or 500), even
 * though the resource may hold a perfectly usable default value or none at
 * all. Since a 404 from `/api/directory/**` for example just means "not in
 * the library yet", callers should be able to treat an errored resource the
 * same as an empty one instead of crashing change detection.
 */
export function safeValue<T>(
  resource: Pick<WritableResource<T>, 'hasValue' | 'value'>,
): T | undefined {
  return resource.hasValue() ? resource.value() : undefined;
}
