import {error} from "@sveltejs/kit";
import {API_URL} from "$env/static/private";

export const GET = async ({ locals, url }) => {
    const user = locals.user
    if (!user) {
        return error(401, { message: 'Unauthorized' })
    }

    const withReset = url.searchParams("reset") ? `/reset` : '';

    const response = await fetch(`${API_URL}/files/local_folder_structure${withReset}`, {
        method: 'GET',
        headers: {
            Authorization: user.session || ''
        }
    })

    return response
}