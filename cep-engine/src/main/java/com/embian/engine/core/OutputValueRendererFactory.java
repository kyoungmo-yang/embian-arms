package com.embian.engine.core;

import com.espertech.esper.event.util.OutputValueRenderer;
import com.espertech.esper.event.util.OutputValueRendererBase;
import com.espertech.esper.event.util.OutputValueRendererJSONString;
import com.espertech.esper.event.util.OutputValueRendererXMLString;
import com.espertech.esper.event.util.RendererMetaOptions;
import com.espertech.esper.util.JavaClassHelper;

public class OutputValueRendererFactory
{
    private static OutputValueRenderer jsonStringOutput = new OutputValueRendererJSONString();
    private static OutputValueRenderer xmlStringOutput = new OutputValueRendererXMLString();
    private static OutputValueRenderer baseOutput = new OutputValueRendererBase();

    /**
     * Returns a renderer for an output value.
     * @param type to render
     * @param options options
     * @return renderer
     */
    protected static OutputValueRenderer getOutputValueRenderer(@SuppressWarnings("rawtypes") Class type, RendererMetaOptions options)
    {
        if (type.isArray())
        {
            type = type.getComponentType();
        }
        if (type == String.class ||
            type == Character.class ||
            type == char.class ||
            type.isEnum() ||
            (!JavaClassHelper.isNumeric(type) && JavaClassHelper.getBoxedType(type) != Boolean.class))
        {
            if (options.isXmlOutput())
            {
                return xmlStringOutput;
            }
            else
            {
                return jsonStringOutput;
            }
        }
        else
        {
            return baseOutput;
        }
    }
}
