import babel from 'rollup-plugin-babel'
import commonjs from '@rollup/plugin-commonjs'
import postcss from 'rollup-plugin-postcss'
import resolve from '@rollup/plugin-node-resolve'
import external from 'rollup-plugin-peer-deps-external'
import img from '@rollup/plugin-image'

import pkg from './package.json'

export default {
	input: './libIndex.js',
	output: [
		{
			file: pkg.main,
			format: 'cjs',
			sourcemap: true
		},
		{
			file: pkg.module,
			format: 'es',
			sourcemap: true
		}
	],
	plugins: [
		img(),
		external(),
		postcss({
			modules: true
		}),
		babel({
			exclude: 'node_modules/**',
			presets: [
	
				"@babel/preset-env", {},
				"@babel/preset-react", {}
			],
			plugins: [
					[
						"@babel/plugin-proposal-class-properties",
						{
							"loose": true
						}
					]
			]
		}),
		resolve({ preferBuiltins: true, mainFields: ['browser'] })
	]
}
