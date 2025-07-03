import {error, json, redirect} from '@sveltejs/kit'
import {handleLoginRedirect} from "$lib/utils";

import fs from 'fs';
import path from 'path';

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