import { API_URL} from '$env/static/private'
import { handleLoginRedirect } from '$lib/utils'
import { fail, redirect } from '@sveltejs/kit'
import { DropboxAuth } from 'dropbox'
import type { PageServerLoad } from './$types'
import { OAuth2Client } from 'google-auth-library'
import { groupStore, labelStore } from '$lib/store'
import { get } from 'svelte/store'

export const load: PageServerLoad = async ({ locals, cookies, url }) => {
	if (!locals.user) {
		redirect(302, handleLoginRedirect(url))
	}

	const labels = await(await fetch(`${API_URL}/users/labels`, {
		method: 'GET',
		headers: {
			Authorization: cookies.get('session') || ''
		}
	})).json()

	const groups: DUUIGroups = await(await fetch(`${API_URL}/users/groups`, {
		method: 'GET',
		headers: {
			Authorization: cookies.get('session') || ''
		}
	})).json()

	let settings: DUUISettings = {
			dbx_app_key: '',
			dbx_app_secret: '',
			dbx_redirect_url: '',
			google_client_id: '',
			google_client_secret: '',
			google_redirect_uri: '',
			allowed_origins: [],
			file_upload_directory: ''
		}

	try {
		settings = await(await fetch(`${API_URL}/settings`, {
			method: 'GET',
			headers: {
				Authorization: cookies.get('session') || ''
			}
		})).json()
	} catch (error) {
		console.error('Error fetching settings')
	}

	// Retrieve the Dropbox OAuth 2.0 credentials from the backend.
	// These correspond to the properties set in the config file.
	const response = await fetch(`${API_URL}/users/auth/dropbox`, {
		method: 'GET'
	})

	let dropbBoxURL = new String('')

	try {
		const credentials: {
			key: string
			secret: string
			url: string
		} = await response.json()

		const dbxAuth = new DropboxAuth({
			clientId: credentials.key,
			clientSecret: credentials.secret
		})

		dropbBoxURL = await dbxAuth.getAuthenticationUrl(
			credentials.url,
			cookies.get('session') || '',
			'code',
			'offline',
			undefined,
			undefined,
			false
		)
	} catch (error) { /* empty */ }


	let googleDriveURL = ""
	const googleResponse = await fetch(`${API_URL}/users/auth/google`, {
		method: 'GET'
	})

	const googleCredentials: {
		key: string
		secret: string
		url: string
	} = await googleResponse.json();

	const googleAuth = new OAuth2Client(
			googleCredentials.key,
			googleCredentials.secret,
			googleCredentials.url)

	googleDriveURL = googleAuth.generateAuthUrl(
		{
						scope: "https://www.googleapis.com/auth/drive openid ",
						access_type: "offline",
						redirect_uri: googleCredentials.url,
						prompt: "select_account"
		})

	/**
	 * Fetch a user from the backend.
	 *
	 * @returns the json object returned from the API call.
	 */
	const fetchProfile = async () => {
		const response = await fetch(`${API_URL}/users/${locals.user?.oid}`, {
			method: 'GET'
		})

		if (!response.ok) {
			return fail(response.status, { message: response.statusText })
		}
		return await response.json()
	}

	/**
	 * Fetch users from the database (only for Admins.)
	 * @returns the json object returned from the API call.
	 */
	const fetchUsers = async () => {
		const response = await fetch(`${API_URL}/users`, {
			method: 'GET',
			headers: {
				Authorization: cookies.get('session') || ''
			}
		})

		
		if (!response.ok) {
			return fail(response.status, { message: response.statusText })
		}

		return await response.json()
	}

	let usr = (await fetchProfile()).user
	let usrs = (await fetchUsers()).users

	return {
		dropbBoxURL: dropbBoxURL,
		googleDriveURL: googleDriveURL,
		user: usr,
		theme: +(cookies.get('theme') || '0'),
		users: usrs,
		labels: labels,
		groups: groups,
		settings: settings,
	}
}