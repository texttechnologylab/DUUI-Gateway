import { API_URL } from '$env/static/private'

/**
 * Try to download a file by sending a request to the backend.
 */
export async function GET({ url, cookies, fetch }) {
	const provider = url.searchParams.get('provider')
	const providerId = url.searchParams.get('provider_id')
	const path = url.searchParams.get('path')
	const pipelineId = url.searchParams.get('pipeline_id')

	const response = await fetch(`${API_URL}/files/preprocess?provider=${provider}&provider_id=${providerId}&path=${path}&pipeline_id=${pipelineId}`, {
		method: 'GET',

		headers: {
			Authorization: cookies.get('session') || ''
		}
	})

	return response
}
