import type { LayoutServerLoad } from './$types'
import { API_URL } from '$env/static/private'
import { error } from '@sveltejs/kit'

export const load: LayoutServerLoad = async ({ locals, cookies }) => {


	const response = await fetch(`${API_URL}/users/${locals.user.role}`, {
		method: 'GET'
	})

	return {
		user: locals.user,
		theme: +(cookies.get('theme') || '0'),
		globals: locals.globals
	}
}
