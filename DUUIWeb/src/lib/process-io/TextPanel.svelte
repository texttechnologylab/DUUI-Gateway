<script lang="ts">
	import TextArea from '$lib/svelte/components/Input/TextArea.svelte'
	import type { DUUIDocumentProvider } from '$lib/duui/io.js'

	export let provider: DUUIDocumentProvider
</script>

<TextArea
	label="Document Text"
	name="content"
	error={provider.content === '' ? 'Text cannot be empty' : ''}
	bind:value={provider.content}
/>
