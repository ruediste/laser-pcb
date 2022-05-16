import React, { useState } from "react";
import { baseUrl } from "./useData";

export default function CameraView() {
    const [frameCount, setFrameCount] = useState(0);
    return <div className="d-flex justify-content-center" style={{}}>
        <div style={{ display: 'inline-block', maxWidth: '100%', position: 'relative' }}>
            <img style={{ width: '100%' }}
                src={baseUrl + 'cncConnection/frame.jpg?c=' + frameCount} onLoad={() => setFrameCount(c => c + 1)} alt="" />

            <div className="d-flex justify-content-center" style={{ position: 'absolute', left: 0, width: '100%', top: 0, height: '100%' }} >
                <svg viewBox="0 0 176.13 178.91" width="100%">
                    <g

                        transform="translate(-180.17 -248.48)"
                    >
                        <path

                            style={{ stroke: "#000000", strokeWidth: 2, fill: 'none' }}
                            d="m247.25 179.95c0 35.589-27.568 64.439-61.575 64.439s-61.575-28.85-61.575-64.439 27.568-64.439 61.575-64.439 61.575 28.85 61.575 64.439z"
                            transform="matrix(1.0697 0 0 1.0697 69.611 145.44)"
                        />
                        <path

                            style={{ stroke: "#000000", strokeWidth: 2, fill: "none" }}
                            d="m247.25 179.95c0 35.589-27.568 64.439-61.575 64.439s-61.575-28.85-61.575-64.439 27.568-64.439 61.575-64.439 61.575 28.85 61.575 64.439z"
                            transform="matrix(.76407 0 0 .76407 126.36 200.44)"
                        />
                        <path

                            style={{ stroke: "#000000", strokeWidth: 2, fill: "none" }}
                            d="m247.25 179.95c0 35.589-27.568 64.439-61.575 64.439s-61.575-28.85-61.575-64.439 27.568-64.439 61.575-64.439 61.575 28.85 61.575 64.439z"
                            transform="matrix(.45844 0 0 .45844 183.11 255.44)"
                        />
                        <rect

                            style={{ stroke: "#000000", strokeLinecap: "round", strokeWidth: 1.216, fill: "#000000" }}
                            height="58.784"
                            width="1.7837"
                            y="249.08"
                            x="267.34"
                        />
                        <rect

                            style={{ stroke: "#000000", strokeWidth: 1.2163, fill: "#000000" }}
                            height="58.784"
                            width="1.7837"
                            y="368"
                            x="267.34"
                        />
                        <rect

                            style={{ stroke: "#000000", strokeWidth: 1.2163, fill: "#000000" }}
                            transform="rotate(90)"
                            height="58.784"
                            width="1.7837"
                            y="-355.69"
                            x="337.04"
                        />
                        <rect

                            style={{ stroke: "#000000", strokeWidth: 1.2163, fill: "#000000" }}
                            transform="rotate(90)"
                            height="58.784"
                            width="1.7837"
                            y="-239.56"
                            x="337.04"
                        />
                        <rect

                            style={{ fill: "#000000" }}
                            height="0.3"
                            width="60"
                            y="337.78"
                            x="238.23"
                        />
                        <rect

                            style={{ fill: "#000000" }}
                            transform="rotate(90)"
                            height="0.3"
                            width="60"
                            y="-268.38"
                            x="307.93"
                        />
                        <rect

                            style={{ fill: "#000000" }}
                            height="1"
                            width="3"
                            y="332.13"
                            x="266.72"
                        />
                        <rect

                            style={{ fill: "#000000" }}
                            height="1"
                            width="3"
                            y="314.13"
                            x="266.72"
                        />
                        <rect

                            style={{ fill: "#000000" }}
                            height="1"
                            width="3"
                            y="320.13"
                            x="266.72"
                        />
                        <rect

                            style={{ fill: "#000000" }}
                            height="1"
                            width="3"
                            y="326.13"
                            x="266.72"
                        />
                        <rect

                            style={{ fill: "#000000" }}
                            height="1"
                            width="3"
                            y="360.74"
                            x="266.75"
                        />
                        <rect

                            style={{ fill: "#000000" }}
                            height="1"
                            width="3"
                            y="342.74"
                            x="266.75"
                        />
                        <rect

                            style={{ fill: "#000000" }}
                            height="1"
                            width="3"
                            y="348.74"
                            x="266.75"
                        />
                        <rect

                            style={{ fill: "#000000" }}
                            height="1"
                            width="3"
                            y="354.74"
                            x="266.75"
                        />
                        <rect

                            style={{ fill: "#000000" }}
                            transform="rotate(90)"
                            height="1"
                            width="3"
                            y="-274.04"
                            x="336.42"
                        />
                        <rect

                            style={{ fill: "#000000" }}
                            transform="rotate(90)"
                            height="1"
                            width="3"
                            y="-292.04"
                            x="336.42"
                        />
                        <rect

                            style={{ fill: "#000000" }}
                            transform="rotate(90)"
                            height="1"
                            width="3"
                            y="-286.04"
                            x="336.42"
                        />
                        <rect

                            style={{ fill: "#000000" }}
                            transform="rotate(90)"
                            height="1"
                            width="3"
                            y="-280.04"
                            x="336.42"
                        />
                        <rect

                            style={{ fill: "#000000" }}
                            transform="rotate(90)"
                            height="1"
                            width="3"
                            y="-245.43"
                            x="336.45"
                        />
                        <rect

                            style={{ fill: "#000000" }}
                            transform="rotate(90)"
                            height="1"
                            width="3"
                            y="-263.43"
                            x="336.45"
                        />
                        <rect

                            style={{ fill: "#000000" }}
                            transform="rotate(90)"
                            height="1"
                            width="3"
                            y="-257.43"
                            x="336.45"
                        />
                        <rect

                            style={{ fill: "#000000" }}
                            transform="rotate(90)"
                            height="1"
                            width="3"
                            y="-251.43"
                            x="336.45"
                        />
                    </g
                    >
                </svg>
            </div>
        </div>
    </div>;
}