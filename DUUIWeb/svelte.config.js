import adapter from '@sveltejs/adapter-node';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';


/** @type {import('@sveltejs/kit').Config} */
const config = {
	extensions: ['.svelte'],
	// Consult https://kit.svelte.dev/docs/integrations#preprocessors
	// for more information about preprocessors
	preprocess: [ vitePreprocess()],
	
	kit: {
		adapter: adapter({
			out: 'build'
		  }),
		  
		files: { 
			assets: 'static'
		 },

		csrf: { checkOrigin: false },

		env: {
			dir: process.cwd()
		  }
	},

};
export default config;