import { API_URL } from '$env/static/private'
import { v4 as uuidv4 } from 'uuid'
import type { PageServerLoad } from './$types'

export const load: PageServerLoad = async ({ url, cookies, fetch }) => {
	const code = url.searchParams.get('code')
	const name = uuidv4()
	const alias = localStorage.getItem("currentDropboxConnectionAlias")

	const response = await fetch(`${API_URL}/users/auth/dropbox?code=${code}&name=${name}&alias=${alias}`, {
		method: 'PUT',
		headers: {
			Authorization: cookies.get('session') || ''
		}
	})

	return {
		success: response.ok
	}
}
