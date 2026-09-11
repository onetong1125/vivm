let csrfToken

export async function request(path, options = {}) {
  const method = (options.method || 'GET').toUpperCase()
  const headers = new Headers(options.headers)

  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    await ensureCsrfToken()
    headers.set(csrfToken.headerName, csrfToken.token)
  }

  const response = await fetch(path, {
    ...options,
    method,
    headers,
    credentials: 'include'
  })

  if (!response.ok) {
    const body = await response.text()
    throw new Error(body || `请求失败（${response.status}）`)
  }

  if (response.status === 204) {
    return undefined
  }

  return response.json()
}

export async function ensureCsrfToken() {
  if (!csrfToken) {
    csrfToken = await request('/api/auth/csrf')
  }
}

export function clearCsrfToken() {
  csrfToken = undefined
}
