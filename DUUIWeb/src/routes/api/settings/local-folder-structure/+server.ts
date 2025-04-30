import {error} from "@sveltejs/kit";
import {API_URL} from "$env/static/private";

export const GET = async ({ locals, url }) => {
    const user = locals.user
    if (!user) {
        return error(401, { message: 'Unauthorized' })
    }

    console.log(url.toString())

    const withReset = url.searchParams.get("reset") ? `reset` : 'cached';

    const response = await fetch(`${API_URL}/files/local-folder-structure/${withReset}`, {
        method: 'GET',
        headers: {
            Authorization: user.session || ''
        }
    })

    if (!response.ok) {
        console.log(response)

    }


    return response
}