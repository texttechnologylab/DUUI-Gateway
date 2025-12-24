<script lang="ts">
	import { IO, PROMPT_EXTENSIONS, splitPromptAttachments, toFileList } from '$lib/duui/io.js'
	import TextPanel from '$lib/process-io/TextPanel.svelte'
	import { FileDropzone } from '@skeletonlabs/skeleton'
	import type { AcceptTypes } from '@skeletonlabs/skeleton'
	import type { DUUIDocumentProvider } from '$lib/duui/io.js'

	export let provider: DUUIDocumentProvider
	export let fileAccept: AcceptTypes
	export let promptAccept: AcceptTypes = PROMPT_EXTENSIONS

	let promptText = ''
	let files: FileList

	export const getPromptText = () => promptText
	export const getFiles = () => files
	export const setFiles = (next: FileList) => (files = next)

	function onPromptFilesDropped(dropped: FileList) {
		const merged = [...Array.from(files ?? []), ...Array.from(dropped)]
		const { wavs, pngs, mp4 } = splitPromptAttachments(toFileList(merged))

		const next: File[] = [...pngs, ...wavs]
		if (mp4) next.push(mp4)

		files = toFileList(next)
	}
</script>

{#if provider.provider === IO.Prompt}
	<div class="space-y-4">
		<TextPanel provider={{ ...provider, content: promptText }} />
		<div class="space-y-1 ">
			<p class="form-label">File</p>
			<FileDropzone
				name="inputFile"
				on:files={(e) => onPromptFilesDropped(e.detail.files)}
				accept={promptAccept}
				multiple={true}
				border="border border-color"
				rounded="rounded-md"
				class="input-wrapper"
			/>
			<p class="form-label {(files?.length || 0) === 0 ? 'text-error-500' : ''}">
				{files?.length || 0} files selected
			</p>
		</div>
	</div>
{:else if provider.provider === IO.File}
	<div class="space-y-1 ">
		<p class="form-label">File</p>
		<FileDropzone
			name="inputFile"
			bind:files
			accept={fileAccept}
			multiple={true}
			border="border border-color"
			rounded="rounded-md"
			class="input-wrapper"
		/>
		<p class="form-label {(files?.length || 0) === 0 ? 'text-error-500' : ''}">
			{files?.length || 0} files selected
		</p>
	</div>
{/if}
