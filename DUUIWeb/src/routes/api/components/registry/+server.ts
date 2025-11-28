import { error, json, redirect } from '@sveltejs/kit'
import { handleLoginRedirect } from '$lib/utils'

import fs from 'fs'
import path from 'path'

type DockerImageReference = {
	host: string
	repository: string
	reference: string
	protocol: 'http' | 'https'
}

const parseDockerImageReference = (imageUri: string): DockerImageReference | null => {
	if (!imageUri?.trim()) return null

	const normalized = imageUri.trim()
	const protocol: 'http' | 'https' = normalized.startsWith('http://') ? 'http' : 'https'
	const cleanTarget = normalized.replace(/^https?:\/\//, '')

	const lastAt = cleanTarget.lastIndexOf('@')
	const lastColon = cleanTarget.lastIndexOf(':')
	const separatorIndex = lastAt > -1 ? lastAt : lastColon
	if (separatorIndex === -1) return null

	const hostAndRepo = cleanTarget.slice(0, separatorIndex)
	const reference = cleanTarget.slice(separatorIndex + 1)
	if (!hostAndRepo || !reference) return null

	const [host, ...repoParts] = hostAndRepo.split('/')
	if (!host || repoParts.length === 0) return null

	const repository = repoParts.join('/')
	return { host, repository, reference, protocol }
}

class TimeoutError extends Error {
	constructor(message = 'Request timed out') {
		super(message)
		this.name = 'TimeoutError'
	}
}

const fetchWithTimeout = async (
	url: string,
	init: RequestInit,
	timeoutMs: number | null
): Promise<Response> => {
	const controller = new AbortController()
	const { signal } = controller
	let timeoutId: ReturnType<typeof setTimeout> | null = null
	let timedOut = false

	if (timeoutMs !== null && Number.isFinite(timeoutMs) && timeoutMs > 0) {
		timeoutId = setTimeout(() => {
			timedOut = true
			controller.abort()
		}, timeoutMs)
	}

	try {
		return await fetch(url, { ...init, signal })
	} catch (error) {
		if (timedOut) {
			throw new TimeoutError()
		}
		throw error
	} finally {
		if (timeoutId) clearTimeout(timeoutId)
	}
}

/**
 * Attempts to fetch component list from the DUUI registry.
 * @returns the list or null.
 */
export const GET = async ({ cookies, fetch, locals, url }) => {
	if (!locals.user) {
		redirect(302, handleLoginRedirect(url))
	}

	const registryEndpint = url.searchParams.get('endpoint')

	if (!registryEndpint) {
		return error(400, { message: 'The registry endpoint is required.' })
	}

	// TEST:
	// try {
	// 	const filePath = path.resolve('src/routes/api/components/registry/test-registry.json');
	// 	const fileContent = fs.readFileSync(filePath, 'utf-8');
	// 	const data = JSON.parse(fileContent);
	//
	// 	return json(data, {
	// 		headers: {
	// 			'Content-Type': 'application/json'
	// 		},
	// 		status: 200
	// 	});
	// } catch (error) {
	// 	return json({ error: 'Failed to load test-registry.json' }, { status: 500 });
	// }

	// REQUEST
	const response = await fetch(`${registryEndpint}/api/images/list`, {
		method: 'GET'
	})

	if (response.ok) {
		return json(await response.json(), {
			headers: {
				'Content-Type': 'application/json'
			},
			status: 200
		})
	}

	return response
}

/**
 * Attempts to fetch component list from the DUUI registry filtered by a search string.
 * @returns the list or null.
 */
export async function PUT({ request, fetch, locals, url}) {
	if (!locals.user) {
		redirect(302, handleLoginRedirect(url))
	}

	const registryEndpint = url.searchParams.get('endpoint')

	if (!registryEndpint) {
		return error(400, { message: 'The registry endpoint is required.' })
	}

	const data = await request.json()
	const registrySearchQuery = data.registrySearchQuery
	const flagName = data.flagName || "true"
	const flagResultingTypes = data.flagResultingTypes || "true"
	const flagRequiredTypes = data.flagRequiredTypes || "true"
	const flagSearchTags = data.flagSearchTags || "true"
	const flagLanguage = data.flagLanguage || "true"
	const flagShortDescription = data.flagShortDescription || "true"


	const response = await fetch(
		`${registryEndpint}api/images/generalSearch/
			${registrySearchQuery}
			?name=${flagName}
			&resultingTypes=${flagResultingTypes}
			&requiredTypes=${flagRequiredTypes}
			&searchTags=${flagSearchTags}
			&language=${flagLanguage}
			&shortDescription=${flagShortDescription}`,
		{
			method: 'GET'
		}
	)

	if (response.ok) {
		return json(await response.json(), {
			headers: {
				'Content-Type': 'application/json'
			},
			status: 200
		})
	}

	return response
}

export async function POST({ request, fetch, locals, url }) {
	if (!locals.user) {
		redirect(302, handleLoginRedirect(url))
	}

	const { imageUri, timeoutMs = 5000 } = await request.json()
	if (!imageUri) {
		return error(400, { message: 'The Docker image target is required.' })
	}

	const parsed = parseDockerImageReference(imageUri)
	if (!parsed) {
		return json(
			{ ok: false, message: 'Invalid Docker image reference provided.' },
			{ status: 200 }
		)
	}

	const manifestUrl = `${parsed.protocol}://${parsed.host}/v2/${parsed.repository}/manifests/${parsed.reference}`

	try {
		const response = await fetchWithTimeout(
			manifestUrl,
			{
				method: 'GET',
				headers: {
					Accept: 'application/vnd.docker.distribution.manifest.v2+json'
				}
			},
			timeoutMs
		)

		if (response.ok) {
			return json({ ok: true }, { status: 200 })
		}

		return json(
			{
				ok: false,
				message: `Registry responded with ${response.status} ${response.statusText}`
			},
			{ status: 200 }
		)
	} catch (err) {
		if (err instanceof TimeoutError) {
			return json({ ok: false, timedOut: true }, { status: 200 })
		}

		return json(
			{
				ok: false,
				message: err instanceof Error ? err.message : 'Failed to reach Docker registry'
			},
			{ status: 200 }
		)
	}
}
