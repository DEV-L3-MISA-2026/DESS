// The backend returns { code, message } for every error (see platform/error/ApiError).
export interface ApiError {
  code: string
  message: string
}

export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`/api${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  })
  if (!response.ok) {
    const error = (await response.json().catch(() => null)) as ApiError | null
    throw error ?? { code: 'HTTP_' + response.status, message: response.statusText }
  }
  return response.status === 204 ? (undefined as T) : ((await response.json()) as T)
}
