import {error} from "@sveltejs/kit";
import {API_URL} from "$env/static/private";

export const GET = async ({ request, locals, url }) => {
    const user = locals.user
    if (!user) {
        return error(401, { message: 'Unauthorized' })
    }
    const data = await request.json()
    const withReset = data.reset ? `/reset` : '';
    
    const response = await fetch(`${API_URL}/files/filtered_folder_structure`, {
        method: 'GET',
        headers: {
            Authorization: user.session || ''
        }
    })

    return response
}